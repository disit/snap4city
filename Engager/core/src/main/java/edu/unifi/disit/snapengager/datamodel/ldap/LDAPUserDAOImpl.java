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
package edu.unifi.disit.snapengager.datamodel.ldap;

import static org.springframework.ldap.query.LdapQueryBuilder.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;

import edu.unifi.disit.snapengager.exception.LDAPException;

public class LDAPUserDAOImpl implements LDAPUserDAO {

	@Autowired
	private MessageSource messages;

	@Autowired
	private LdapTemplate ldapTemplate;

	private static final Logger logger = LogManager.getLogger();

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

		// ou has always to exist
		int startindexOU = groupname.indexOf("ou=");
		if (startindexOU == -1)
			return false;

		int endindexOU = groupname.indexOf(",", startindexOU + 3);
		if (endindexOU == -1)
			return false;

		String ouGroupname = groupname.substring(startindexOU + 3, endindexOU);

		if (startindexCN != -1) {
			// group scenario
			int endindexCN = groupname.indexOf(",", startindexCN + 3);

			if (endindexCN == -1) {
				return false;
			}

			String cnGroupname = groupname.substring(startindexCN + 3, endindexCN);

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
		} else {
			// organization scenario
			return ldapTemplate
					.search(query()
							.attributes("ou")
							.where("objectClass").is("organizationalUnit").and("ou").is(ouGroupname),
							new AttributesMapper<String>() {
								public String mapFromAttributes(Attributes attrs) throws NamingException {
									return attrs.get("ou").get().toString();
								}
							})
					.size() > 0;
		}
	}

	@Override
	public List<String> getEmails(String username) {
		return ldapTemplate.search(query()
				.attributes("mail")
				.where("objectClass").is("inetOrgPerson").and("cn").is(username),
				new AttributesMapper<String>() {
					public String mapFromAttributes(Attributes attrs) throws NamingException {
						if ((attrs != null) && (attrs.get("mail") != null) && (attrs.get("mail").get() != null))
							return attrs.get("mail").get().toString();
						else
							return null;
					}
				});
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

		return extractUsernames(returno.get(0), lang);
	}

	List<String> extractUsernames(List<String> toremove, Locale lang) throws NoSuchMessageException, LDAPException {
		List<String> toreturn = new ArrayList<String>();
		for (String s : toremove) {
			if (s.length() != 0)
				toreturn.add(extractUsername(s, lang));
		}
		return toreturn;
	}

	String extractUsername(String toremove, Locale lang) throws NoSuchMessageException, LDAPException {

		int startindexCN = toremove.indexOf("cn=");
		if (startindexCN == -1) {
			throw new LDAPException(messages.getMessage("ldap.ko.nocnspecified", null, lang));
		}

		int endindexCN = toremove.indexOf(",", startindexCN + 3);

		if (endindexCN == -1) {
			throw new LDAPException(messages.getMessage("ldap.ko.nocnspecified", null, lang));
		}
		return toremove.substring(startindexCN + 3, endindexCN);
	}

	public List<String> getAllPersonNames() {
		return ldapTemplate.search(
				query().where("objectclass").is("inetOrgPerson"),
				new AttributesMapper<String>() {
					public String mapFromAttributes(Attributes attrs) throws NamingException {
						return attrs.get("cn").get().toString();
					}
				});
	}

	@Override
	public List<LDAPEntity> getAllOrganization(Locale lang) {
		return ldapTemplate.search(
				query().where("objectClass").is("organizationalUnit"),
				new AttributesMapper<LDAPEntity>() {
					public LDAPEntity mapFromAttributes(Attributes attrs) throws NamingException {
						String name = attrs.get("ou").get().toString();
						List<String> usernames = new ArrayList<String>();
						if (attrs.get("l") != null) {
							NamingEnumeration<?> i = attrs.get("l").getAll();
							while (i.hasMore()) {
								// TODO extract CN
								String toextract = (String) i.next();
								if (toextract.length() != 0)
									usernames.add(extractUsername(toextract, lang).toLowerCase());
							}
						}
						return new LDAPEntity(name, usernames);
					}
				});
	}

	@Override
	public List<LDAPEntity> getAllRoles(Locale lang) {
		return ldapTemplate.search(
				query().where("objectClass").is("organizationalRole"),
				new AttributesMapper<LDAPEntity>() {
					public LDAPEntity mapFromAttributes(Attributes attrs) throws NamingException {
						String name = attrs.get("cn").get().toString();

						logger.debug("name is: {}", name);

						List<String> usernames = new ArrayList<String>();
						if (attrs.get("roleOccupant") != null) {
							NamingEnumeration<?> i = attrs.get("roleOccupant").getAll();
							while (i.hasMore()) {
								// TODO extract CN
								String toextract = (String) i.next();
								if (toextract.length() != 0)
									usernames.add(extractUsername(toextract, lang).toLowerCase());
							}
						}
						return new LDAPEntity(name, usernames);
					}
				});
	}

	@Override
	public List<LDAPEntity> getAllGroups(Locale lang) {
		return ldapTemplate.search(
				query().where("objectClass").is("groupOfNames"),
				new AttributesMapper<LDAPEntity>() {
					public LDAPEntity mapFromAttributes(Attributes attrs) throws NamingException {
						String name = attrs.get("cn").get().toString();
						List<String> usernames = new ArrayList<String>();
						if (attrs.get("member") != null) {
							NamingEnumeration<?> i = attrs.get("member").getAll();
							while (i.hasMore()) {
								// TODO extract CN
								String toextract = (String) i.next();
								if (toextract.length() != 0)
									usernames.add(extractUsername(toextract, lang).toLowerCase());
							}
						}
						return new LDAPEntity(name, usernames);
					}
				});
	}
}