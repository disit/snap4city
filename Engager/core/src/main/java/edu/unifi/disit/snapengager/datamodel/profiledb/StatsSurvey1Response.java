/* Snap4City Engager (SE)
   Copyright (C) 2015 DISIT Lab http://www.disit.org - University of Florence
   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU Affero General Public License as
   published by the Free Software Foundation, either version 3 of the
   License, or (at your option) any later version.
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU Affero General Public License for more details.
   You should have received a copy of the GNU Affero General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package edu.unifi.disit.snapengager.datamodel.profiledb;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "stats_survey1_response")
public class StatsSurvey1Response {

	private static final Logger logger = LogManager.getLogger();

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(name = "question_name")
	private String questionName;
	private String organization;
	@Type(type = "timestamp")
	private Date created = new Date();
	private Integer answer1 = 0;
	private Integer answer2 = 0;
	private Integer answer3 = 0;
	private Integer answer4 = 0;
	private Integer answer5 = 0;
	private Integer answer6 = 0;
	private Integer answer7 = 0;
	private Integer answer8 = 0;
	private Integer answer9 = 0;
	private Integer answer10 = 0;
	@OneToMany(mappedBy = "statsSurvey1Response", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	private Set<StatsSurvey1ResponseText> contributions;

	public StatsSurvey1Response() {
		super();
	}

	public StatsSurvey1Response(String organization, String questionName) {
		this(organization, questionName, new Date());
	}

	public StatsSurvey1Response(String organization, String questionName, Date created) {
		super();
		this.organization = organization;
		this.questionName = questionName;
		this.created = created;
	}

	public StatsSurvey1Response(String questionName, String organization, Date created, Integer answer1, Integer answer2, Integer answer3, Integer answer4, Integer answer5, Integer answer6, Integer answer7, Integer answer8,
			Integer answer9, Integer answer10) {
		super();
		this.questionName = questionName;
		this.organization = organization;
		this.created = created;
		this.answer1 = answer1;
		this.answer2 = answer2;
		this.answer3 = answer3;
		this.answer4 = answer4;
		this.answer5 = answer5;
		this.answer6 = answer6;
		this.answer7 = answer7;
		this.answer8 = answer8;
		this.answer9 = answer9;
		this.answer10 = answer10;
	}

	public StatsSurvey1Response(Long id, String questionName, String organization, Date created, Integer answer1, Integer answer2, Integer answer3, Integer answer4, Integer answer5, Integer answer6, Integer answer7, Integer answer8,
			Integer answer9, Integer answer10) {
		super();
		this.id = id;
		this.questionName = questionName;
		this.organization = organization;
		this.created = created;
		this.answer1 = answer1;
		this.answer2 = answer2;
		this.answer3 = answer3;
		this.answer4 = answer4;
		this.answer5 = answer5;
		this.answer6 = answer6;
		this.answer7 = answer7;
		this.answer8 = answer8;
		this.answer9 = answer9;
		this.answer10 = answer10;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getQuestionName() {
		return questionName;
	}

	public void setQuestionName(String questionName) {
		this.questionName = questionName;
	}

	public String getOrganization() {
		return organization;
	}

	public void setOrganization(String organization) {
		this.organization = organization;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public Integer getAnswer1() {
		return answer1;
	}

	public void setAnswer1(Integer answer1) {
		this.answer1 = answer1;
	}

	public Integer getAnswer2() {
		return answer2;
	}

	public void setAnswer2(Integer answer2) {
		this.answer2 = answer2;
	}

	public Integer getAnswer3() {
		return answer3;
	}

	public void setAnswer3(Integer answer3) {
		this.answer3 = answer3;
	}

	public Integer getAnswer4() {
		return answer4;
	}

	public void setAnswer4(Integer answer4) {
		this.answer4 = answer4;
	}

	public Integer getAnswer5() {
		return answer5;
	}

	public void setAnswer5(Integer answer5) {
		this.answer5 = answer5;
	}

	public Integer getAnswer6() {
		return answer6;
	}

	public void setAnswer6(Integer answer6) {
		this.answer6 = answer6;
	}

	public Integer getAnswer7() {
		return answer7;
	}

	public void setAnswer7(Integer answer7) {
		this.answer7 = answer7;
	}

	public Integer getAnswer8() {
		return answer8;
	}

	public void setAnswer8(Integer answer8) {
		this.answer8 = answer8;
	}

	public Integer getAnswer9() {
		return answer9;
	}

	public void setAnswer9(Integer answer9) {
		this.answer9 = answer9;
	}

	public Integer getAnswer10() {
		return answer10;
	}

	public void setAnswer10(Integer answer10) {
		this.answer10 = answer10;
	}

	public Set<StatsSurvey1ResponseText> getContributions() {
		return contributions;
	}

	public void setContributions(Set<StatsSurvey1ResponseText> contributions) {
		this.contributions = contributions;
	}

	public void addContribution(StatsSurvey1ResponseText contribution) {
		if (this.contributions == null)
			this.contributions = new HashSet<StatsSurvey1ResponseText>();
		this.contributions.add(contribution);
	}

	public void addContribution(String contribution) {
		StatsSurvey1ResponseText ssrt = new StatsSurvey1ResponseText();
		ssrt.setContribution(contribution);
		ssrt.setStatsSurvey1Response(this);
		addContribution(ssrt);
	}

	@Override
	public String toString() {
		return "StatsSurvey1Response [id=" + id + ", questionName=" + questionName + ", organization=" + organization + ", created=" + created + ", answer1=" + answer1 + ", answer2=" + answer2 + ", answer3=" + answer3 + ", answer4="
				+ answer4 + ", answer5=" + answer5 + ", answer6=" + answer6 + ", answer7=" + answer7 + ", answer8=" + answer8 + ", answer9=" + answer9 + ", answer10=" + answer10 + ", contributions=" + contributions + "]";
	}

	public void addAnswer(String answer) {
		switch (answer) {
		case "item1":
			this.answer1++;
			break;
		case "item2":
			this.answer2++;
			break;
		case "item3":
			this.answer3++;
			break;
		case "item4":
			this.answer4++;
			break;
		case "item5":
			this.answer5++;
			break;
		case "item6":
			this.answer6++;
			break;
		case "item7":
			this.answer7++;
			break;
		case "item8":
			this.answer8++;
			break;
		case "item9":
			this.answer9++;
			break;
		case "item10":
			this.answer10++;
			break;
		default:
			logger.debug("answer {} is not an item", answer);
			addContribution(answer);
			break;
		}
	}

	public void addAnswer(Integer answer) {

		switch (answer) {
		case 1:
			this.answer1++;
			break;
		case 2:
			this.answer2++;
			break;
		case 3:
			this.answer3++;
			break;
		case 4:
			this.answer4++;
			break;
		case 5:
			this.answer5++;
			break;
		case 6:
			this.answer6++;
			break;
		case 7:
			this.answer7++;
			break;
		case 8:
			this.answer8++;
			break;
		case 9:
			this.answer9++;
			break;
		case 10:
			this.answer10++;
			break;
		default:
			logger.warn("answer {} not recognized", answer);
			break;
		}
	}

	public void addAnswers(List<String> answers) {
		for (String answer : answers) {
			addAnswer(answer);
		}
	}

}