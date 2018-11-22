/* Data Manager (DM).
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
package edu.unifi.disit.datamanager.datamodel.ldap;

import static org.springframework.ldap.query.LdapQueryBuilder.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
//import javax.naming.ldap.LdapName;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;

import edu.unifi.disit.datamanager.exception.LDAPException;
//import org.springframework.ldap.support.LdapNameBuilder;

public class LDAPUserDAOImpl implements LDAPUserDAO {

	@Autowired
	private MessageSource messages;

	@Autowired
	private LdapTemplate ldapTemplate;

	private static final Logger logger = LogManager.getLogger();

	// @Override
	// public void create(LDAPUser person) {
	// ldapTemplate.create(person);
	// }
	//
	// @Override
	// public void update(LDAPUser person) {
	// ldapTemplate.update(person);
	// }
	//
	// @Override
	// public void delete(LDAPUser person) {
	// ldapTemplate.delete(ldapTemplate.findByDn(buildDn(person), LDAPUser.class));
	// }
	//

	@Override
	public List<String> getGroupAndOUnames(String username) {
		List<String> toreturn = getGroupnames(username);
		toreturn.addAll(getOUnames(username));
		return toreturn;
	}

	@Override
	public List<String> getOUnames(String username) {
		return ldapTemplate.search(query()
				.attributes("ou")
				.where("objectClass").is("organizationalUnit").and("l").is("cn=" + username + "," + ((LdapContextSource) ldapTemplate.getContextSource()).getBaseLdapPathAsString()),
				new AttributesMapper<String>() {
					public String mapFromAttributes(Attributes attrs) throws NamingException {
						return "ou=" + attrs.get("ou").get().toString() + "," + ((LdapContextSource) ldapTemplate.getContextSource()).getBaseLdapPathAsString();
					}
				});
	}

	@Override
	public List<String> getGroupnames(String username) {
		return ldapTemplate.search(query()
				.attributes("cn", "ou")
				.where("objectClass").is("groupOfNames").and("member").is("cn=" + username + "," + ((LdapContextSource) ldapTemplate.getContextSource()).getBaseLdapPathAsString()),
				new AttributesMapper<String>() {
					public String mapFromAttributes(Attributes attrs) throws NamingException {
						if ((attrs.get("cn") != null) && (attrs.get("ou") != null))
							return "cn=" + attrs.get("cn").get().toString() + ",ou=" + attrs.get("ou").get().toString() + "," + ((LdapContextSource) ldapTemplate.getContextSource()).getBaseLdapPathAsString();
						else { // cn is always present since it's in the query!
							logger.warn("The retreived cn {} hasn't any attached ou", attrs.get("cn").get().toString());
							return "cn=" + attrs.get("cn").get().toString() + "," + ((LdapContextSource) ldapTemplate.getContextSource()).getBaseLdapPathAsString();
						}

					}
				});
	}

	@Override
	public boolean usernameExist(String username) {
		return ldapTemplate
				.search(query()
						.attributes("cn")
						.where("objectClass").is("inetOrgPerson").and("cn").is(username),
						new AttributesMapper<String>() {
							public String mapFromAttributes(Attributes attrs) throws NamingException {
								return attrs.get("cn").get().toString();
							}
						})
				.size() > 0;
	}

	@Override
	public boolean groupnameExist(String groupname) {

		int startindexCN = groupname.indexOf("cn=");
		if (startindexCN == -1)
			return false;

		int endindexCN = groupname.indexOf(",", startindexCN + 3);
		if (endindexCN == -1)
			return false;

		int startindexOU = groupname.indexOf("ou=", endindexCN);
		if (startindexOU == -1)
			return false;

		int endindexOU = groupname.indexOf(",", startindexOU + 3);
		if (endindexOU == -1)
			return false;

		String cnGroupname = groupname.substring(startindexCN + 3, endindexCN);
		String ouGroupname = groupname.substring(startindexOU + 3, endindexOU);

		return ldapTemplate
				.search(query()
						.attributes("cn")
						.where("objectClass").is("groupOfNames").and("cn").is(cnGroupname).and("ou").is(ouGroupname),
						new AttributesMapper<String>() {
							public String mapFromAttributes(Attributes attrs) throws NamingException {
								return attrs.get("cn").get().toString();
							}
						})
				.size() > 0;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<String> getUsernamesFromGroup(String groupnamefilter, Locale lang) throws LDAPException {

		int startindexCN = groupnamefilter.indexOf("cn=");

		List<List<String>> returno = new ArrayList<List<String>>();

		// ou has always to exist
		int startindexOU = groupnamefilter.indexOf("ou=");
		if (startindexOU == -1) {
			throw new LDAPException(messages.getMessage("ldap.ko.noouspecified", null, lang));
		}
		int endindexOU = groupnamefilter.indexOf(",", startindexOU + 3);
		if (endindexOU == -1) {
			throw new LDAPException(messages.getMessage("ldap.ko.noouspecified", null, lang));
		}

		String ouGroupname = groupnamefilter.substring(startindexOU + 3, endindexOU);

		if (startindexCN != -1) {
			// group scenario

			int endindexCN = groupnamefilter.indexOf(",", startindexCN + 3);

			if (endindexCN == -1) {
				throw new LDAPException(messages.getMessage("ldap.ko.nocnspecified", null, lang));
			}

			String cnGroupname = groupnamefilter.substring(startindexCN + 3, endindexCN);

			returno = ldapTemplate.search(query()
					.attributes("member")
					.where("objectClass").is("groupOfNames").and("cn").is(cnGroupname).and("ou").is(ouGroupname),
					new AttributesMapper<List<String>>() {
						public List<String> mapFromAttributes(Attributes attrs) throws NamingException {
							NamingEnumeration<String> all = (NamingEnumeration<String>) attrs.get("member").getAll();
							List<String> result = new ArrayList<>();
							while (all.hasMore())
								result.add(all.next());
							return result;
						}
					});
		} else {
			// ou scenario

			returno = ldapTemplate.search(query()
					.attributes("l")
					.where("objectClass").is("organizationalUnit").and("ou").is(ouGroupname),
					new AttributesMapper<List<String>>() {
						public List<String> mapFromAttributes(Attributes attrs) throws NamingException {
							NamingEnumeration<String> all = (NamingEnumeration<String>) attrs.get("l").getAll();
							List<String> result = new ArrayList<>();
							while (all.hasMore())
								result.add(all.next());
							return result;
						}
					});
		}

		if (returno.size() == 0)
			throw new LDAPException(messages.getMessage("ldap.ko.nofound", null, lang));
		if (returno.size() > 1)
			throw new LDAPException(messages.getMessage("ldap.ko.toomany", null, lang));

		return extractUsername(returno.get(0), lang);

	}

	List<String> extractUsername(List<String> toremove, Locale lang) throws NoSuchMessageException, LDAPException {
		List<String> toreturn = new ArrayList<String>();
		for (String s : toremove) {
			int startindexCN = s.indexOf("cn=");
			if (startindexCN == -1) {
				throw new LDAPException(messages.getMessage("ldap.ko.nocnspecified", null, lang));
			}

			int endindexCN = s.indexOf(",", startindexCN + 3);

			if (endindexCN == -1) {
				throw new LDAPException(messages.getMessage("ldap.ko.nocnspecified", null, lang));
			}
			toreturn.add(s.substring(startindexCN + 3, endindexCN));
		}
		return toreturn;
	}

	// @Override
	// public List<LDAPUser> findAll() {
	// return ldapTemplate.findAll(LDAPUser.class);
	// }
	//
	// @Override
	// public LDAPUser findByPrimaryKey(String country, String company, String fullname) {
	// LdapName dn = buildDn(country, company, fullname);
	// LDAPUser person = ldapTemplate.findByDn(dn, LDAPUser.class);
	//
	// return person;
	// }
	//
	// private LdapName buildDn(LDAPUser person) {
	// return buildDn(person.getCountry(), person.getCompany(), person.getFullName());
	// }
	//
	// private LdapName buildDn(String country, String company, String fullname) {
	// return LdapNameBuilder.newInstance()
	// .add("c", country)
	// .add("ou", company)
	// .add("cn", fullname)
	// .build();
	// }

}
