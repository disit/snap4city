/* Data Manager (DM).
   Copyright (C) 2015 DISIT Lab http://www.disit.org - University of Florence
   This program is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public License
   as published by the Free Software Foundation; either version 2
   of the License, or (at your option) any later version.
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA. */
package edu.unifi.disit.datamanager.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import edu.unifi.disit.datamanager.datamodel.elasticdb.KPIElasticValue;
import edu.unifi.disit.datamanager.datamodel.elasticdb.KPIElasticValueDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIValue;
import edu.unifi.disit.datamanager.datamodel.profiledb.KPIValueDAO;
import edu.unifi.disit.datamanager.datamodel.profiledb.OwnershipDAO;
import edu.unifi.disit.datamanager.eventDrivenMessages.KafkaProducerConfig;
import edu.unifi.disit.datamanager.exception.CredentialsException;
import edu.unifi.disit.datamanager.exception.DataNotValidException;

@Service
public class KPIValueServiceImpl implements IKPIValueService {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	private MessageSource messages;

	@Autowired
	KPIValueDAO kpiValueRepository;

	@Autowired
	KPIElasticValueDAO kpiElasticValueRepository;

	@Autowired
	ElasticsearchRestTemplate esRestTemplate;

	@Autowired
	OwnershipDAO ownershipRepo;

	@Autowired
	IDelegationService delegationService;

	@Autowired
	ICredentialsService credentialsService;

	@Autowired
	BCryptPasswordEncoder bCryptPasswordEncoder;

	@PersistenceContext
	private EntityManager entityManager;
	
	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	@Override
	public KPIValue getKPIValueById(Long id, Locale lang) throws CredentialsException {
		logger.debug("getKPIValueById INVOKED on id {}", id);
		return kpiValueRepository.findById(id).orElse(null);
	}

	@Override
	public Page<KPIValue> findByKpiId(Long kpiId, Pageable pageable) throws CredentialsException {
		logger.debug("findAllByKpiId INVOKED on kpiId {}", kpiId);
		return kpiValueRepository.findByKpiIdAndDeleteTimeIsNull(kpiId, pageable);
	}

	@Override
	public Page<KPIValue> findByKpiIdFiltered(Long kpiId, String searchKey, Pageable pageable)
			throws CredentialsException {
		logger.debug("findAllFilteredByKpiId INVOKED on searchKey {} kpiId {}", searchKey, kpiId);
		return kpiValueRepository.findByKpiIdAndValueContainingAllIgnoreCaseAndDeleteTimeIsNull(kpiId, searchKey,
				pageable);
	}

	@Override
	public List<KPIValue> findByKpiIdNoPages(Long kpiId) {
		logger.debug("findByKpiIdNoPages INVOKED on kpiId {}", kpiId);
		return kpiValueRepository.findByKpiIdAndDeleteTimeIsNull(kpiId);
	}

	@Override
	public List<KPIValue> findByKpiIdGeoLocated(Long kpiId) throws CredentialsException {
		logger.debug("findByKpiIdGeoLocated INVOKED on kpiId {}", kpiId);
		return kpiValueRepository
				.findByKpiIdAndDeleteTimeIsNullAndLatitudeIsNotNullAndLongitudeIsNotNullAndLatitudeNotLikeAndLongitudeNotLike(
						kpiId, "", "");
	}

	@Override
	public List<KPIValue> findByKpiIdFilteredNoPages(Long kpiId, String searchKey) {
		logger.debug("findAllFilteredByKpiId INVOKED on searchKey {} kpiId {}", searchKey, kpiId);
		return kpiValueRepository.findByKpiIdAndValueContainingAllIgnoreCaseAndDeleteTimeIsNull(kpiId, searchKey);
	}

	@Override
	public List<KPIValue> findByKpiIdNoPagesWithLimit(Long kpiId, Date from, Date to, Integer first, Integer last,
			Locale lang) throws DataNotValidException {
		logger.debug("findByKpiIdNoPagesWithLimit INVOKED on kpiId {}, from {}, to {}, first {}, last {}", kpiId, from,
				to, first, last);

		if ((first != null) && (last != null)) {
			throw new DataNotValidException(messages.getMessage("getdata.ko.firstandlastspecified", null, lang));
		}

		return kpiValueRepository.findByKpiIdNoPagesWithLimit(kpiId, from, to, first, last);
	}

	@Override
	public KPIValue saveKPIValue(KPIValue kpivalue) throws CredentialsException {
		logger.debug("saveKPIValue INVOKED on kpivalue {}", kpivalue.getId());
		kpivalue.setInsertTime(new Date());
		kpivalue = kpiValueRepository.save(kpivalue);
		if (Boolean.TRUE.equals(KafkaProducerConfig.getSendMessageOnEventDriveMessages())) {
			try {
			sendMessageOnEventDriveMessages(KafkaProducerConfig.getPrefixTopic() + kpivalue.getKpiId().toString(),
					kpivalue);
			} catch (Exception e) {
				logger.error("Kafka Problem", e.getMessage());
			}
		}
		return kpivalue;
	}

	@Override
	public List<KPIValue> saveKPIValueList(List<KPIValue> kpiValueList) throws CredentialsException {
		logger.debug("saveKPIValueList");
		List<KPIValue> list = new ArrayList<>();
		kpiValueRepository.saveAll(kpiValueList).forEach(list::add);
		return list;
	}

	@Override
	public void deleteKPIValue(Long id) throws CredentialsException {
		logger.debug("deleteKPIValue INVOKED on id {}", id);
		kpiValueRepository.deleteById(id);

	}

	@Override
	public List<Date> getKPIValueDates(Long kpiId) throws CredentialsException {
		logger.debug("getKPIValueDates INVOKED on kpiId {}", kpiId);
		return kpiValueRepository.findByKpiIdDistinctDateAndDeleteTimeIsNull(kpiId);
	}

	@Override
	public List<Date> getKPIValueDatesCoordinatesOptionallyNull(Long kpiId) throws CredentialsException {
		logger.debug("getKPIValueDates INVOKED on kpiId {}", kpiId);
		return kpiValueRepository.findByKpiIdDistinctDateAndDeleteTimeIsNullWithCoordinatesOptionallyNull(kpiId);
	}

	@Override
	public KPIElasticValue getKPIElasticValueById(String id, Locale lang) throws CredentialsException {
		logger.debug("getKPIValueById INVOKED on id {}", id);
		this.setCorrectIndex();
		return kpiElasticValueRepository.findById(id).orElse(null);
	}

	@Override
	public Page<KPIElasticValue> findBySensorId(Long kpiId, Pageable pageable) throws CredentialsException {
		logger.debug("findAllByKpiId INVOKED on kpiId {}", kpiId);
		this.setCorrectIndex();
		return kpiElasticValueRepository.findBySensorId(kpiId, pageable);
	}

	@Override
	public Page<KPIElasticValue> findBySensorIdFiltered(Long kpiId, String searchKey, Pageable pageable)
			throws CredentialsException {
		logger.debug("findAllFilteredByKpiId INVOKED on searchKey {} kpiId {}", searchKey, kpiId);
		this.setCorrectIndex();
		return kpiElasticValueRepository.findBySensorIdAndValueContainingAllIgnoreCase(kpiId, searchKey, pageable);
	}

	@Override
	public List<KPIElasticValue> findBySensorIdNoPages(Long kpiId) {
		logger.debug("findByKpiIdNoPages INVOKED on kpiId {}", kpiId);
		this.setCorrectIndex();
		return kpiElasticValueRepository.findBySensorId(kpiId);
	}

	/*
	 * @Override public List<KPIElasticValue> findBySensorIdGeoLocated(Long kpiId)
	 * throws CredentialsException {
	 * logger.debug("findByKpiIdGeoLocated INVOKED on kpiId {}", kpiId); return
	 * kpiElasticValueRepository.
	 * findBySensorIdAndLatitudeIsNotNullAndLongitudeIsNotNullAndLatitudeNotLikeAndLongitudeNotLike
	 * (kpiId, "", ""); }
	 */

	@Override
	public List<KPIElasticValue> findBySensorIdFilteredNoPages(Long kpiId, String searchKey) {
		logger.debug("findAllFilteredByKpiId INVOKED on searchKey {} kpiId {}", searchKey, kpiId);
		this.setCorrectIndex();
		return kpiElasticValueRepository.findBySensorIdAndValueContainingAllIgnoreCase(kpiId, searchKey);
	}

	@Override
	public KPIElasticValue saveKPIElasticValue(KPIElasticValue kpiElasticValue) throws CredentialsException {
		logger.debug("saveKPIValue INVOKED on kpivalue {}", kpiElasticValue.getId());
		this.setCorrectIndex();
		kpiElasticValue = kpiElasticValueRepository.save(kpiElasticValue);
		if (Boolean.TRUE.equals(KafkaProducerConfig.getSendMessageOnEventDriveMessages())) {
			sendMessageOnEventDriveMessages(
					KafkaProducerConfig.getPrefixTopic() + kpiElasticValue.getSensorId(), kpiElasticValue);
		}
		return kpiElasticValue;
	}

	@Override
	public List<KPIElasticValue> saveKPIElasticValueList(List<KPIElasticValue> kpiElasticValueList)
			throws CredentialsException {
		logger.debug("saveKPIElasticValueList");
		List<KPIElasticValue> list = new ArrayList<>();
		this.setCorrectIndex();
		kpiElasticValueRepository.saveAll(kpiElasticValueList).forEach(list::add);
		return list;
	}

	@Override
	public void deleteKPIElasticValue(String id) throws CredentialsException {
		logger.debug("deleteKPIValue INVOKED on id {}", id);
		this.setCorrectIndex();
		kpiElasticValueRepository.deleteById(id);
	}

	@Override
	public List<KPIElasticValue> deleteKPIElasticValuesOfKpiId(Long kpiId) throws CredentialsException {
		logger.debug("deleteKPIValue INVOKED on id {}", kpiId);
		this.setCorrectIndex();
		return kpiElasticValueRepository.deleteBySensorId(kpiId);
	}

	@Override
	public List<KPIElasticValue> findBySensorIdNoPagesWithLimit(Long sensorId, Date from, Date to, Integer first,
			Integer last, Locale lang) throws DataNotValidException {
		logger.debug("findBySensorIdNoPagesWithLimit INVOKED on kpiId {}, from {}, to {}, first {}, last {}", sensorId,
				from, to, first, last);

		if ((first != null) && (last != null)) {
			throw new DataNotValidException(messages.getMessage("getdata.ko.firstandlastspecified", null, lang));
		}

		this.setCorrectIndex();
		return kpiElasticValueRepository.findBySensorIdNoPagesWithLimit(sensorId.toString(), from, to, first, last);
	}

	@Override
	public List<String> getKPIElasticValueDates(Long kpiId, boolean checkCoordinates) throws CredentialsException {
		logger.debug("getKPIElasticValueDates INVOKED on kpiId {}", kpiId);
		return kpiElasticValueRepository.getElasticValuesDates(kpiId.toString(), checkCoordinates);
	}

	private void setCorrectIndex() {
		/*
		 * String correctIndex = "sensorinew3-" +
		 * credentialsService.getOrganizationUnit(new Locale("en"));
		 * ConfigIndexBean.setIndexName(correctIndex); if
		 * (!esRestTemplate.indexExists(correctIndex)) {
		 * esRestTemplate.createIndex(correctIndex);
		 * esRestTemplate.putMapping(KPIElasticValue.class); }
		 */
	}

	private void sendMessageOnEventDriveMessages(String topicName, Object obj) {
		ListenableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topicName, obj);

		future.addCallback(new ListenableFutureCallback<SendResult<String, Object>>() {

			@Override
			public void onSuccess(SendResult<String, Object> result) {
				logger.debug("Sent message=[topicName=" + topicName + ",content=" + obj.toString() + "] with offset=["
						+ result.getRecordMetadata().offset() + "]");
			}

			@Override
			public void onFailure(Throwable ex) {
				logger.debug("Unable to send message=[topicName=" + topicName + ",content=" + obj.toString()
						+ "] due to : " + ex.getMessage());
			}
		});
	}

}