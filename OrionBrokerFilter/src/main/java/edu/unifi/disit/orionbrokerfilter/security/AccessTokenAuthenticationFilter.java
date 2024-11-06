/* Orion Broker Filter (OBF).
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
package edu.unifi.disit.orionbrokerfilter.security;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.unifi.disit.orionbrokerfilter.datamodel.CachedCredentials;
import edu.unifi.disit.orionbrokerfilter.datamodel.Certified;
import edu.unifi.disit.orionbrokerfilter.datamodel.CheckCredential;
import edu.unifi.disit.orionbrokerfilter.datamodel.Credentials;
import edu.unifi.disit.orionbrokerfilter.datamodel.DelegationPublic;
import edu.unifi.disit.orionbrokerfilter.datamodel.Ownership;
import edu.unifi.disit.orionbrokerfilter.datamodel.Response;
import edu.unifi.disit.orionbrokerfilter.exception.CredentialsNotValidException;
import java.util.Objects;

@Component
public class AccessTokenAuthenticationFilter extends GenericFilterBean {

	private static final Logger logger = LogManager.getLogger();

	@Value("${spring.openidconnect.clientid}")
	private String clientId;

	@Value("${spring.openidconnect.username}")
	private String username;

	@Value("${spring.openidconnect.password}")
	private String password;

	@Value("${spring.openidconnect.token_endpoint}")
	private String token_endpoint;

	@Value("${spring.ownership_endpoint}")
	private String ownership_endpoint;

	@Value("${spring.delegation_endpoint}")
	private String delegation_endpoint;

	@Value("${spring.servicemapkb_endpoint:#{null}}")
	private String servicemapkb_endpoint;

	@Value("${spring.prefix_serviceuri}")
	private String prefixServiceUri;

	@Value("${spring.organization}")
	private String organization;

	@Value("${spring.context_broker_name}")
	private String contextBrokerName;

	@Value("${spring.elapsingcache.minutes}")
	private Integer minutesElapsingCache;

	@Value("${multitenancy:false}")
	private Boolean multitenancy;

      	@Value("${use_blockchain:false}")
	private Boolean use_blockchain;

	@Autowired
	private MessageSource messages;

	Certified certified;

	ObjectMapper objectMapper = new ObjectMapper();

	// used for IOT-BUTTON like scenario
	HashMap<String, CachedCredentials> cachedCredentials = new HashMap<String, CachedCredentials>();
	HashMap<String, Ownership> cachedCredentialsOwnership = new HashMap<String, Ownership>();
	HashMap<String, String> cachedPksha1UsernameOwnership = new HashMap<String, String>();

	static HashMap<String,Certified> certifiedDevice= new HashMap<String,Certified>();

	public static Certified getCertifiedDevice(String elementId){
		synchronized (certifiedDevice) {
			return certifiedDevice.get(elementId);
		}
	}

	// used for EDGE-CLOUD scenario
	Map<String, ArrayList<CheckCredential>> cachedDelegation = new HashMap<String, ArrayList<CheckCredential>>();
	Map<String, DelegationPublic> cachedDelegationPublic = new HashMap<String, DelegationPublic>();
	Map<String, ArrayList<CheckCredential>> cachedOwnership = new HashMap<String, ArrayList<CheckCredential>>();

	@Autowired
	private RestTemplate restTemplate;

	String refreshToken = null;

	static Map<String, Object> certInfos = null;

	@SuppressWarnings("unchecked")
	@PostConstruct
	private void postConstruct() {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws JsonProcessingException, IOException, ServletException {

		final HttpServletRequest req = (HttpServletRequest) request;
		MultiReadHttpServletRequest multiReadRequest = new MultiReadHttpServletRequest((HttpServletRequest) request);

                //SKIP checks if is OPTIONS request
                if(req.getMethod().equals("OPTIONS")) {
                  filterChain.doFilter(multiReadRequest, response);
                  return;
                }

		// retrieve https certicate
		String pksha1 = null;
		X509Certificate[] certs = (X509Certificate[]) multiReadRequest.getAttribute("javax.servlet.request.X509Certificate");
		if ((certs != null) && (certs.length > 0)) {
			String pk = new String(Base64.encode(certs[0].getPublicKey().getEncoded()), StandardCharsets.UTF_8);
			logger.debug("certificate arrived, public key is:" + pk);
			pksha1 = DigestUtils.sha1Hex(pk.getBytes());
			logger.debug("sha1 public key is: {}", pksha1);
		}

		// retrieve k1 k2 credentials
		String k1 = multiReadRequest.getParameter("k1");
		String k2 = multiReadRequest.getParameter("k2");

		// retrieve NGSI info
		String queryType = ((HttpServletRequest) request).getServletPath();
		String elementId = req.getParameter("elementid");// mandatory

		// eventually enrich with MultiTenancy/ServicePath info
		if (multitenancy) {// check always to be made
			if (req.getHeader("Fiware-ServicePath") != null)
				elementId = req.getHeader("Fiware-ServicePath") + "." + elementId;
			else
				elementId = "/." + elementId;
			if (req.getHeader("Fiware-Service") != null)
				elementId = req.getHeader("Fiware-Service") + "." + elementId;
			else
				elementId = "." + elementId;

			logger.debug("elementid became:" + elementId);
		}

		// retrieve eventually accessToken
		String requestedAccessToken = null;
		if ((req.getHeader("Authorization") != null) && (req.getHeader("Authorization").length() > 8)) {
			requestedAccessToken = req.getHeader("Authorization").substring(7);
			logger.debug("accessToken arrived:" + requestedAccessToken);
		}

		// retrieve NGSI version
		String version = null;
		if (req.getRequestURL().toString().indexOf("/v1/") >= 0)
			version = "v1";
		else if (req.getRequestURL().toString().indexOf("/v2/") >= 0)
			version = "v2";
		logger.debug("Version is:" + version);

		if ((elementId != null)) {

			logger.debug("Received a request of type {} for {}", queryType, elementId);
			if (k1 != null)
				logger.debug("K1 {}", k1);
			if (k2 != null)
				logger.debug("K2 {}", k2);

			try {

				String sensorName = null;

				if ("v1".equals(version)) {
					logger.debug("Searching sensor name in API v1 body.");
					// passing the original elementId, without any path conversion...
					sensorName = getSensorNameV1(multiReadRequest, isWriteQuery(queryType, req, version, multiReadRequest.getLocale()), req.getParameter("elementid"));// can return null, the passed elementid is the original one
				} else if ("v2".equals(version)) {
					logger.debug("Searching sensor name in API v2 body.");
					sensorName = getSensorNameV2(multiReadRequest, req);// can return null, the passed elementid is the original one
				} else
					throw new CredentialsNotValidException(messages.getMessage("login.ko.requesturlmalformed", null, multiReadRequest.getLocale()));

				String elementType = "IOTID";
				if (sensorName != null) {
					elementType = "ServiceURI";
					logger.debug("sensor's name {} - It's a serviceURI", sensorName);
				}

                                if(use_blockchain) {
                                        if(!certifiedDevice.containsKey(elementId)){
                                                Certified certificationCheck=new Certified();
                                                certificationCheck.setBearerToken("bearer "+requestedAccessToken);
                                                certificationCheck.setDeviceType(elementType);
                                                certificationCheck.setOrganization(organization);
                                                synchronized (certifiedDevice){
                                                        certifiedDevice.put(elementId,certificationCheck);
                                                }
                                        }
                                        if(!Objects.equals(certifiedDevice.get(elementId).getBearerToken(), "bearer " + requestedAccessToken)){
                                                logger.debug("Access token updated");
                                                certifiedDevice.get(elementId).setBearerToken("bearer "+requestedAccessToken);
                                        }
                                }
                                
				checkAuthorization(organization + ":" + contextBrokerName + ":" + elementId, elementType, sensorName, k1, k2, pksha1, requestedAccessToken, queryType, version, req, request.getLocale());

				logger.debug("Credentials ARE VALID");

				authUser();

			} catch (CredentialsNotValidException e) {
				logger.warn("Credentials ARE NOT VALID", e);

				writeResponseError(response, e.getMessage());

				return;
			}

		} else {
			logger.warn("Missing parameter: elementid");

			writeResponseError(response, messages.getMessage("login.ko.missingparameterelementid", null, request.getLocale()));

			return;

		}

		filterChain.doFilter(multiReadRequest, response);// DO WE NEED IT???
	}

	@SuppressWarnings("unchecked")
	private PublicKey retrievePublicKeyFromCertsEndpoint(JWSHeader jwsHeader) {
		try {
                        if(certInfos == null) {
                            synchronized(AccessTokenAuthenticationFilter.class) {
                              if(certInfos == null) {
                                ObjectMapper om = new ObjectMapper();
                                try {
                                        certInfos = om.readValue(new URL(token_endpoint.substring(0, token_endpoint.lastIndexOf("/")) + "/certs").openStream(), Map.class);
                                } catch (Exception e) {
                                        logger.error("Cannot retrieve the certInfo", e);
                                        throw e;
                                }
                              }
                            }
                        }

			List<Map<String, Object>> keys = (List<Map<String, Object>>) certInfos.get("keys");

			Map<String, Object> keyInfo = null;
			for (Map<String, Object> key : keys) {
				String kid = (String) key.get("kid");

				if (jwsHeader.getKeyId().equals(kid)) {
					keyInfo = key;
					break;
				}
			}

			if (keyInfo == null) {
				return null;
			}

			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			String modulusBase64 = (String) keyInfo.get("n");
			String exponentBase64 = (String) keyInfo.get("e");

			// see org.keycloak.jose.jwk.JWKBuilder#rs256
			java.util.Base64.Decoder urlDecoder = java.util.Base64.getUrlDecoder();
			BigInteger modulus = new BigInteger(1, urlDecoder.decode(modulusBase64));
			BigInteger publicExponent = new BigInteger(1, urlDecoder.decode(exponentBase64));

			return keyFactory.generatePublic(new RSAPublicKeySpec(modulus, publicExponent));

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private boolean refreshTokenExpired() {
		if (refreshToken == null)
			return true;

		try {
			TokenVerifier<RefreshToken> verifier = TokenVerifier.create(refreshToken, RefreshToken.class);
			RefreshToken token = verifier
					.publicKey(retrievePublicKeyFromCertsEndpoint(verifier.getHeader()))
					.verify()
					.getToken();
			return token.isExpired();
		} catch (VerificationException e) {
			return true;
		}
	}

	private String retrieveUserName(String requestAccessToken, Locale lang) throws NoSuchMessageException, CredentialsNotValidException {

		if (requestAccessToken == null)
			return null;

		String toreturn = null;

		try {
			TokenVerifier<AccessToken> verifier = TokenVerifier.create(requestAccessToken, AccessToken.class);
			AccessToken token = verifier
					.publicKey(retrievePublicKeyFromCertsEndpoint(verifier.getHeader()))
					.verify()
					.getToken();
			if (!token.isExpired()) {
				toreturn = token.getPreferredUsername();
				if (toreturn == null) {
					Map<String, Object> otherclaims = token.getOtherClaims();
					toreturn = (String) otherclaims.get("username");

				}
			} else {
				logger.warn("The passed accessToken is Expired");
				throw new CredentialsNotValidException(messages.getMessage("login.ko.accesstokennotvalid", null, lang));
			}
		} catch (VerificationException e) {
			logger.warn("Verification failed", e);
			throw new CredentialsNotValidException(messages.getMessage("login.ko.accesstokennotvalid", null, lang));
		}

		logger.debug("Retrieved username is:" + toreturn);

		return toreturn;
	}

	private String getSensorNameV1(HttpServletRequest multiReadRequest, boolean isWriteQuery, String elementId) throws IOException, NoSuchMessageException, CredentialsNotValidException {

		String entityBody = IOUtils.toString(multiReadRequest.getInputStream(), StandardCharsets.UTF_8.toString());

		logger.debug("searching sensor name in --{}--", entityBody);

		// retrieve "attributes index
		int startIndex = entityBody.indexOf("attributes");
		if (startIndex == -1) {
			logger.warn(messages.getMessage("login.ko.sensornamenotpresent", null, multiReadRequest.getLocale()));
			// throw new CredentialsNotValidException(messages.getMessage("login.ko.sensornamenotpresent", null, multiReadRequest.getLocale()));
			return null;
		}

		// calcolate startindex of sensor name
		if (isWriteQuery)
			startIndex = startIndex + 10 + 4 + 8;
		else {
			startIndex = startIndex + 10 + 3;
			if (entityBody.indexOf("\"", startIndex) == startIndex) {
				startIndex++;
			}
		}

		if (startIndex > entityBody.length()) {
			logger.warn(messages.getMessage("login.ko.sensornamenotvalid", null, multiReadRequest.getLocale()) + "(1) entityBody is {}", entityBody);
			// throw new CredentialsNotValidException(messages.getMessage("login.ko.sensornamenotvalid", null, multiReadRequest.getLocale()) + "(1)");
			return null;
		}

		int endIndex = entityBody.indexOf("\"", startIndex);
		if (endIndex == -1) {
			// if the attribute field is empty, the sensor name is empty
			logger.debug("detecting empty attributes, sensor name is empty-string (the delegation has to be formatted correctly)");
			return null;
		}

		if (endIndex - startIndex >= 3) {
			// a sensor name is retrieved -> it's a query or an update!
			// ensure the elementid passed as parameter is included in the entityBody
			if (entityBody.indexOf(elementId) == -1) {
				logger.warn(messages.getMessage("login.ko.elementidnotvalid", null, multiReadRequest.getLocale()) + "(2) entityBody is {}", entityBody);
				throw new CredentialsNotValidException(messages.getMessage("login.ko.elementidnotvalid", null, multiReadRequest.getLocale()));
			}

			return entityBody.substring(startIndex, endIndex);
		} else {
			return null;// no sensor name are retrieved -> it's a subscription or unsubscription!
		}
	}

	private String getSensorNameV2(HttpServletRequest multiReadRequest, HttpServletRequest req) throws IOException, NoSuchMessageException, CredentialsNotValidException {
		String attribute = null;
		int attributeStart;
		int attributeEnd;
		String entityBody = IOUtils.toString(multiReadRequest.getInputStream(), StandardCharsets.UTF_8.toString()).replace(" ", "").replace("\n", "").replace("\r", "");

		try {
			switch (req.getMethod()) {
			case "GET":// Query
				attribute = req.getParameter("attrs");
				if (attribute.indexOf(',') != -1) {
					attribute = attribute.substring(0, attribute.indexOf(','));// consider just the first one
				}
				;
				return attribute;
			case "POST":// Subscription
				int notificationIndex = entityBody.indexOf("notification");
				int attrsIndex = entityBody.indexOf("attrs", notificationIndex);
				attributeStart = entityBody.indexOf("[\"", attrsIndex) + 2;
				attributeEnd = entityBody.indexOf("\"", attributeStart);
				attribute = entityBody.substring(attributeStart, attributeEnd);
				if (entityBody.indexOf(req.getParameter("elementid")) == -1) {
					logger.warn(messages.getMessage("login.ko.elementidnotvalid", null, multiReadRequest.getLocale()) + " entityBody is {}", entityBody);
					throw new CredentialsNotValidException(messages.getMessage("login.ko.elementidnotvalid", null, multiReadRequest.getLocale()));
				}
                                if(attribute.isEmpty())
                                  attribute = null;
				return attribute;
			case "PATCH":// Update
				attributeStart = entityBody.indexOf("{\"") + 2;
				attributeEnd = entityBody.indexOf("\":");
				attribute = entityBody.substring(attributeStart, attributeEnd).trim();
				return attribute;
			case "DELETE":// Unsubscription
				return null;
			}
		} catch (Exception err) {
			logger.warn("Attribute name not detected!");
			return null;
		}
		return null;
	}

	private void writeResponseError(ServletResponse response, String msg) throws JsonProcessingException, IOException {
		Response toreturn2 = new Response();
		toreturn2.setResult(false);
		toreturn2.setMessage(msg);

		((HttpServletResponse) response).setStatus(401);
		((HttpServletResponse) response).getWriter().write(objectMapper.writeValueAsString(toreturn2));
	}

	private void authUser() {
		List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
		authorities.add(new SimpleGrantedAuthority("ROLE_USER")); // here we just set the basic role to USER
		UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken("username", "userpwd", authorities);// here we just set a fake password
		SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
	}

	private void checkAuthorization(String elementId, String elementType, String sensorName, String k1, String k2, String pksha1, String requestedAccessToken, String queryType, String version, HttpServletRequest req, Locale lang)
			throws CredentialsNotValidException, UnsupportedEncodingException {

		if (requestedAccessToken != null)// CLOUD-EDGE scenario
			checkAuthorizationWithAccessToken(elementId, elementType, sensorName, requestedAccessToken, queryType, version, req, lang);
		else// IOT-BUTTON like scenario
			checkAuthorizationWithK1K2(elementId, elementType, sensorName, k1, k2, pksha1, queryType, version, req, lang);
	}

	private CheckCredential retrieveCachedOwnership(String elementId, String elementType, String username, String accessToken, Locale lang) throws CredentialsNotValidException {

		// retrieve cached credentials for Ownership
                CheckCredential o = null;
                ArrayList<CheckCredential> ownerships;
                synchronized(cachedOwnership) {
                        ownerships = cachedOwnership.get(elementId);

                        if (ownerships == null) {
                                logger.debug("ownership not found in cache");
                                ownerships = new ArrayList<CheckCredential>();
                        } else {
                                // retrieve ownership for current username
                                o = ownerships.stream().filter(x -> username.equals(x.getUsername()) && elementType.equals(x.getElementType())).findAny().orElse(null);
                                logger.debug("ownership found in cache: {}", o);

                                if ((o != null) && (o.isElapsed())) {
                                        logger.debug("ownership removed from cache since not valid anymore");
                                        ownerships.remove(o);
                                        cachedOwnership.put(elementId, ownerships);
                                        o = null;
                                }
                        }

                        // if ownership not found or invalidated, retrieve new credentials for ownership
                        if (o == null) {
                                logger.debug("retrieving credentials for ownership");
                                o = getOwnershipCC(accessToken, elementId, elementType, username, lang);
                                ownerships.add(o);
                                cachedOwnership.put(elementId, ownerships);
                        }
                }

		return o;
	}

	private CheckCredential retrieveCachedDelegation(String elementId, String elementType, String username, String accessToken, Locale lang) throws CredentialsNotValidException, UnsupportedEncodingException {
                CheckCredential d = null;
                ArrayList<CheckCredential> delegations;

                synchronized(cachedDelegation) {
                    // retrieve all cached credentials for Delegation
                    delegations = cachedDelegation.get(elementId);

                    if (delegations == null) {
                            logger.debug("delegation not found in cache");
                            delegations = new ArrayList<CheckCredential>();
                    } else {
                            // retrieve delegation for current username
                            d = delegations.stream().filter(x -> username.equals(x.getUsername()) && elementType.equals(x.getElementType())).findAny().orElse(null);
                            logger.debug("delegation found in cache: {}", d);
                            if ((d != null) && (d.isElapsed())) {
                                    logger.debug("d removed from cache since not valid anymore");
                                    delegations.remove(d);
                                    cachedDelegation.put(elementId, delegations);
                                    d = null;
                            }
                    }

                    // if delegation not found or invalidated, retrieve new credentials for delegation
                    if (d == null) {
                            logger.debug("retrieving credentials for delegation");
                            d = getDelegationCC(accessToken, elementId, elementType, username, lang);
                            delegations.add(d);
                            cachedDelegation.put(elementId, delegations);
                    }
                }

		return d;
	}

	private void checkAuthorizationWithAccessToken(String elementId, String elementType, String sensorName, String accessToken, String queryType, String version, HttpServletRequest req, Locale lang)
			throws CredentialsNotValidException, UnsupportedEncodingException {

		String requestUsername = retrieveUserName(accessToken, lang);

		CheckCredential o = retrieveCachedOwnership(elementId, "IOTID", requestUsername, accessToken, lang);

		if ((o != null) && (o.getResult())) {
			logger.debug("Got ownership");
			return;
		} else {
			String sensorUri = (sensorName == null) ? elementId : prefixServiceUri + "/" + contextBrokerName + "/" + organization + "/" + elementId.substring(elementId.lastIndexOf(":") + 1) + "/" + sensorName;
			CheckCredential d = retrieveCachedDelegation(sensorUri, elementType, requestUsername, accessToken, lang);
			boolean canWriteByDelegation = (d!=null && d.getResult() && (d.getKind().equals("READ_WRITE") || d.getKind().equals("WRITE_ONLY") || d.getKind().equals("MODIFY") ));

			if (isWriteQuery(queryType, req, version, lang)) {
				if (canWriteByDelegation) {
					logger.debug("Write OPERATION, got delegation");
					return;
				} else {
					logger.debug("Write OPERATION, not got ownership");
					throw new CredentialsNotValidException(messages.getMessage("login.ko.credentialsnotvalid", null, lang));
				}
			} else {
				if ((d != null) && d.getResult()) {
                                        if(!d.getKind().equals("WRITE_ONLY")) {
					logger.debug("Read OPERATION, got delegation");
					return;
				} else {
                                            logger.debug("Read OPERATION, delegation WRITE ONLY");
                                            throw new CredentialsNotValidException(messages.getMessage("login.ko.credentialsnotvalid", null, lang));
                                        }
				} else {
					logger.debug("Read OPERATION, not got ownership");
					// if the sensorUri is public and not elapsed, authorize
					DelegationPublic dp = cachedDelegationPublic.get(sensorUri);
					if ((dp != null) && (elementType.equalsIgnoreCase(dp.getElementType())))
						if (cachedDelegationPublic.get(sensorUri).isElapsed())// if elapsed remove and continue
							cachedDelegationPublic.remove(sensorUri);
						else {
							logger.debug("Read OPERATION on public, passed");
							return;
						}

					if ((d != null) && (d.getResult() && !"WRITE_ONLY".equals(d.getKind()))) {
						logger.debug("Got READ public delegation");
					} else {
						logger.debug("NOT Got READ public delegation");
						throw new CredentialsNotValidException(messages.getMessage("login.ko.credentialsnotvalid", null, lang));
					}
				}
			}
		}
	}

	private void checkAuthorizationWithK1K2(String elementId, String elementType, String sensorName, String k1, String k2, String pksha1, String queryType, String version, HttpServletRequest req, Locale lang)
			throws CredentialsNotValidException, UnsupportedEncodingException {

		CachedCredentials cc = getCachedCredentials(elementId, elementType, sensorName, lang);

		// enforcement
		if (cc.getIsPublic()) {
			if (isWriteQuery(queryType, req, version, lang)) {
				logger.debug("The operation is WRITE on public");
				if (cc.getOwnerCredentials().isValid(k1, k2, pksha1)) {
					logger.debug("The owner credentials are valid");
					return;
				} else {
					logger.debug("The owner credential are NOT valid");
					throw new CredentialsNotValidException(messages.getMessage("login.ko.ownercredentialsnotvalid", null, lang));
				}
			} else {
				logger.debug("The operation is READ on public");
				return;
			}
		} else {
			if (isWriteQuery(queryType, req, version, lang)) {
				logger.debug("The operation is WRITE on private");
				if (cc.getOwnerCredentials().isValid(k1, k2, pksha1)) {
					logger.debug("The owner credentials are valid");
					return;
				} else {
					logger.debug("The owner credentials are NOT valid");
					throw new CredentialsNotValidException(messages.getMessage("login.ko.ownercredentialsnotvalid", null, lang));
				}
			} else {
				logger.debug("The operation is READ on private");
				if (cc.getOwnerCredentials().isValid(k1, k2, pksha1)) {
					logger.debug("The owner credentials are valid");
					return;
				} else {
					// TODO if the elementID is private, the invoked query is a READ, and it's not the owner, first we need to check if the sensorID is PUBLIC (from cached and via packet inspection) and if not we need to validate the
					// delegation as below
					logger.debug("The owner credentials are not valid, check if there are any delegation");
					for (Credentials c : cc.getDelegatedCredentials()) {
						if (cc.getOwnerCredentials().getPksha1() == null) {// if the elementID is not protected with certificate, use k1, k2 enforcement
							if (c.isValid(k1, k2, null)) {
								logger.debug("One of the delegated credentials are valid, certificate not involved");
								return;
							}
						} else {// if the elementID is protected with certificate: check requestedUsername OR check the username delegated is the same of the username included in the certicate
							if ((c.isValidUsername(getUsername(pksha1, lang)))) {
								logger.debug("One of the delegated credentials are valid, certificate involved");
								return;
							}
						}
					}
					logger.debug("None of the delegated credential are valid");
					throw new CredentialsNotValidException(messages.getMessage("login.ko.ownercredentialsnotvalid", null, lang));
				}
			}
		}
	}

	private String getUsername(String pksha1, Locale lang) throws CredentialsNotValidException {

		if (pksha1 == null)
			return null;

		String toreturn = null;
		if ((toreturn = cachedPksha1UsernameOwnership.get(pksha1)) != null)
			return toreturn;
		else {
			String accessToken = getAccessToken(lang);
			toreturn = getUsernamePKCredentials(accessToken, pksha1, lang);
			if (toreturn != null)
				cachedPksha1UsernameOwnership.put(pksha1, toreturn);

		}
		return toreturn;
	}

	private boolean isWriteQuery(String queryType, HttpServletRequest req, String version, Locale lang) throws CredentialsNotValidException {
		if ("v1".equals(version)) {
			return queryType.contains("updateContext");
		} else if ("v2".equals(version)) {
			return req.getMethod().equals("PATCH");
		} else
			throw new CredentialsNotValidException(messages.getMessage("login.ko.versionnotrecognized", null, lang));
	}

	private CachedCredentials getCachedCredentials(String elementId, String elementType, String sensorName, Locale lang) throws CredentialsNotValidException, UnsupportedEncodingException {

		String accessToken = null;

		Ownership o = cachedCredentialsOwnership.get(elementId);
		if (o == null) {
			logger.debug("ownership not found in cache");
		} else {
			logger.debug("ownership found in cache: {}", o);
			if (o.isElapsed()) {
				logger.debug("ownership remove from cache since not valid anymore");
				cachedCredentialsOwnership.remove(elementId);
				o = null;
			}
		}

		if (o == null) {
			accessToken = getAccessToken(lang);
			o = getOwnership(accessToken, elementId, lang);
			cachedCredentialsOwnership.put(elementId, o);
		}

		String sensorUri = (sensorName == null) ? elementId : o.getElementUrl() + "/" + sensorName;

		// retrieve cached credentials
		CachedCredentials cc = cachedCredentials.get(sensorUri);

		if (cc == null) {
			logger.debug("not found in cache");
		} else {
			logger.debug("found in cache: {}", cc);
			if (cc.isElapsed()) {
				logger.debug("remove from cache since not valid anymore");
				cachedCredentials.remove(sensorUri);
				cc = null;
			}
		}

		// if not found or invalidated, retrieve new credentials
		if (cc == null) {
			logger.debug("retrieving credentials");

			cc = new CachedCredentials(minutesElapsingCache);
			if (servicemapkb_endpoint != null)// if the servicemap endpoint is set, enable the check on public/private
				cc.setIsPublic(isPublicFromKB(o.getElementUrl(), lang));

			cc.setOwnerCredentials(o);

			if (accessToken == null)
				accessToken = getAccessToken(lang);

			cc = enrichDelegatedCredentials(cc, accessToken, sensorUri, elementType, lang);

			cachedCredentials.put(sensorUri, cc);
		}

		logger.debug("Cached credentials are: {}", cc);

		return cc;
	}

	// default value:private (old scenario from iotdirectory)
	// if there is a string that contains "public", in the ow field, it is considered public
	private boolean isPublicFromKB(String elementUrl, Locale lang) throws CredentialsNotValidException {

		String queryKB = "select distinct ?ow { " +
				"OPTIONAL {<" + elementUrl + "> km4c:ownership ?ow.}" + "}";// assume that any elementId here are on this orion broker

		MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
		params.add("format", "json");
		params.add("timeout", "0");
		params.add("query", queryKB);

		UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(servicemapkb_endpoint)
				.queryParams(params)
				.build();
		logger.debug("query isPublicFromKB {}", uriComponents.toUri());

		// RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

		try {
			ResponseEntity<String> response = restTemplate.exchange(uriComponents.toUri(), HttpMethod.GET, entity, String.class);
			logger.debug("Response from isPublicFromKB {}", response);

			ObjectMapper objectMapper = new ObjectMapper();

			if ((response.getBody() == null)) {
				logger.debug("no body found, it's private");
				return false;
			}

			JsonNode rootNode = objectMapper.readTree(response.getBody().getBytes());

			JsonNode resultNode = rootNode.path("results");
			if ((resultNode == null) || (resultNode.isNull()) || (resultNode.isMissingNode())) {
				logger.debug("no results found, it's private");
				return false;
			}

			JsonNode bindingsNode = resultNode.path("bindings");
			if ((bindingsNode == null) || (bindingsNode.isNull()) || (bindingsNode.isMissingNode())) {
				logger.debug("no bindings found, it's private");
				return false;
			}

			Iterator<JsonNode> els = bindingsNode.elements();

			if (els.hasNext()) {
				JsonNode owNode = els.next().path("ow");
				if ((owNode == null) || (owNode.isNull()) || (owNode.isMissingNode())) {
					logger.debug("no ow found, it's private");
					return false;
				}

				JsonNode valueNode = owNode.path("value");
				if ((valueNode == null) || (valueNode.isNull()) || (valueNode.isMissingNode())) {
					logger.debug("no value found, it's private");
					return false;
				}

				return valueNode.asText().contains("public");

			} else {
				logger.debug("bindings empty, it's private");
				return false;
			}

		} catch (HttpClientErrorException | IOException e) {
			logger.error("Trouble in isPublicFromKB", e);
			throw new CredentialsNotValidException(messages.getMessage("login.ko.networkproblems", null, lang));
		}
	}

	private String getAccessToken(Locale lang) throws CredentialsNotValidException {

		if (refreshTokenExpired()) {
			logger.debug("Refresh token is expired...");
			getRefreshToken(lang);
		}

		String toreturn = new String();

		MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
		params.add("grant_type", "refresh_token");
		params.add("refresh_token", refreshToken);
		params.add("client_id", clientId);
		params.add("username", username);
		params.add("password", password);

		UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(token_endpoint).build();
		logger.debug("query getAccessToken {}", uriComponents.toUri());

		// RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(params, headers);

		try {
			ResponseEntity<String> response = restTemplate.exchange(uriComponents.toUri(), HttpMethod.POST, entity, String.class);
			logger.debug("Response from getAccessToken {}", response);

			ObjectMapper objectMapper = new ObjectMapper();

			JsonNode rootNode = objectMapper.readTree(response.getBody().getBytes());
			JsonNode atNode = rootNode.path("access_token");

			if ((atNode == null) || (atNode.isNull()) || (atNode.isMissingNode())) {
				refreshToken = null; // force to re-login
				logger.error("The retrieved data does not contains access_token");
				throw new CredentialsNotValidException(messages.getMessage("login.ko.configurationerror", null, lang));
			}

			JsonNode rtNode = rootNode.path("refresh_token");

			if ((rtNode == null) || (rtNode.isNull()) || (rtNode.isMissingNode())) {
				refreshToken = null; // force to re-login
				logger.error("The retrieved data does not contains refresh_token");
				throw new CredentialsNotValidException(messages.getMessage("login.ko.configurationerror", null, lang));
			}

			refreshToken = rtNode.asText();
			toreturn = atNode.asText();
		} catch (HttpClientErrorException | IOException e) {
			refreshToken = null; // force to re-login
			logger.error("Trouble in getRefresh", e);
			throw new CredentialsNotValidException(messages.getMessage("login.ko.networkproblems", null, lang));
		}

		return toreturn;
	}

	public void getRefreshToken(Locale lang) throws CredentialsNotValidException {

		MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
		params.add("grant_type", "password");
		params.add("client_id", clientId);
		params.add("username", username);
		params.add("password", password);

		UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(token_endpoint).build();
		logger.debug("Query getRefreshToken {}", uriComponents.toUri());

		// RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(params, headers);

		try {

			ResponseEntity<String> response = restTemplate.exchange(uriComponents.toUri(), HttpMethod.POST, entity, String.class);
			logger.debug("Response from getRefreshToken {}", response);

			ObjectMapper objectMapper = new ObjectMapper();

			JsonNode rootNode = objectMapper.readTree(response.getBody().getBytes());
			JsonNode atNode = rootNode.path("refresh_token");

			if ((atNode == null) || (atNode.isNull()) || (atNode.isMissingNode())) {
				logger.error("The retrieved data does not contains access_token");
				throw new CredentialsNotValidException(messages.getMessage("login.ko.configurationerror", null, lang));
			}

			refreshToken = atNode.asText();
		} catch (HttpClientErrorException | IOException e) {
			logger.error("Trouble in getAccessToken", e);
			throw new CredentialsNotValidException(messages.getMessage("login.ko.networkproblems", null, lang));
		}
	}

	private Ownership getOwnership(String accessToken, String elementId, Locale lang) throws CredentialsNotValidException {

		Ownership toreturn = null;

		MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
		params.add("type", "IOTID");
		params.add("accessToken", accessToken);
		params.add("elementId", elementId);

		UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(ownership_endpoint)
				.queryParams(params)
				.build();
		logger.debug("query getOwnerCredentials {}", uriComponents.toUri());

		// RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

		try {
			ResponseEntity<String> response = restTemplate.exchange(uriComponents.toUri(), HttpMethod.GET, entity, String.class);
			logger.debug("Response from  getOwnerCredentials {}", response);

			ObjectMapper objectMapper = new ObjectMapper();

			JsonNode rootNode = objectMapper.readTree(response.getBody().getBytes());
			Iterator<JsonNode> els = rootNode.elements();

			if ((els == null) || (!els.hasNext())) {
				logger.error("The retrieved data does not contains any elements");
				throw new CredentialsNotValidException(messages.getMessage("login.ko.configurationerror", null, lang));
			}

			JsonNode elNode = els.next();

			// username - has to be present
			JsonNode usernameNode = elNode.path("username");
			if ((usernameNode == null) || (usernameNode.isNull()) || (usernameNode.isMissingNode())) {
				logger.error("The retreived data does not contains username");
				throw new CredentialsNotValidException(messages.getMessage("login.ko.configurationerror", null, lang));
			}

			// elementUrl - has to be present
			JsonNode elementUrlNode = elNode.path("elementUrl");
			if ((elementUrlNode == null) || (elementUrlNode.isNull()) || (elementUrlNode.isMissingNode())) {
				logger.error("The retreived data does not contains elementUrlNode");
				throw new CredentialsNotValidException(messages.getMessage("login.ko.configurationerror", null, lang));
			}

			// elementDetails - has to be present
			JsonNode edNode = elNode.path("elementDetails");
			if ((edNode == null) || (edNode.isNull()) || (edNode.isMissingNode())) {
				logger.error("The retreived data does not contains elementDetails");
				throw new CredentialsNotValidException(messages.getMessage("login.ko.configurationerror", null, lang));
			}

			// k1, k2 - has to be present
			JsonNode k1Node = edNode.path("k1");
			JsonNode k2Node = edNode.path("k2");
			if ((k1Node == null) || (k1Node.isNull()) || ((k2Node == null) || (k2Node.isNull())) || (k1Node.isMissingNode()) || (k2Node.isMissingNode())) {
				logger.error("The retreived data does not contains k1, k2");
				throw new CredentialsNotValidException(messages.getMessage("login.ko.configurationerror", null, lang));
			}

                        if(use_blockchain) {
                                JsonNode certNode = edNode.path("Certified");
                                if ((certNode == null) || (certNode.isNull()) || (certNode.isMissingNode())){
                                        logger.info("Device not certified");
                                        certifiedDevice.get(elementId).setCertified(false);
                                }else{
                                        logger.info("Device is certified");
                                        certifiedDevice.get(elementId).setCertified(true);
                                }
                        }
			// publickeySHA1 - can be missing
			String pksha1 = null;
			JsonNode pksha1Node = elNode.path("publickeySHA1");
			if ((pksha1Node != null) && (!pksha1Node.isNull()) && (!pksha1Node.isMissingNode())) {
				pksha1 = pksha1Node.asText();
			}

			toreturn = new Ownership(elementUrlNode.asText(), k1Node.asText(), k2Node.asText(), usernameNode.asText(), pksha1, minutesElapsingCache);
		} catch (HttpClientErrorException | IOException e) {
			logger.error("Trouble in getOwnerCredentials", e);
			throw new CredentialsNotValidException(messages.getMessage("login.ko.networkproblems", null, lang));
		}

		return toreturn;
	}

	private String getUsernamePKCredentials(String accessToken, String pksha1, Locale lang) throws CredentialsNotValidException {

		String toreturn = null;

		MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
		params.add("type", "IOTID");
		params.add("accessToken", accessToken);
		params.add("pubkeySHA1", pksha1);

		UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(ownership_endpoint)
				.queryParams(params)
				.build();
		logger.debug("query getOwnerCredentials {}", uriComponents.toUri());

		// RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

		try {
			ResponseEntity<String> response = restTemplate.exchange(uriComponents.toUri(), HttpMethod.GET, entity, String.class);
			logger.debug("Response from getOwnerCredentials {}", response);

			ObjectMapper objectMapper = new ObjectMapper();

			JsonNode rootNode = objectMapper.readTree(response.getBody().getBytes());
			Iterator<JsonNode> els = rootNode.elements();

			if ((els == null) || (!els.hasNext())) {
				logger.error("The retrieved data does not contains any elements");
				throw new CredentialsNotValidException(messages.getMessage("login.ko.configurationerror", null, lang));
			}

			JsonNode elNode = els.next();
			JsonNode usernameNode = elNode.path("username");

			if ((usernameNode == null) || (usernameNode.isNull()) || (usernameNode.isMissingNode())) {
				logger.error("The retreived data does not contains usernmae");
				throw new CredentialsNotValidException(messages.getMessage("login.ko.configurationerror", null, lang));
			}

			toreturn = usernameNode.asText();


		} catch (HttpClientErrorException | IOException e) {
			logger.error("Trouble in getOwnerCredentials", e);
			throw new CredentialsNotValidException(messages.getMessage("login.ko.networkproblems", null, lang));
		}

		return toreturn;
	}

	private CachedCredentials enrichDelegatedCredentials(CachedCredentials cc, String accessToken, String sensorUri, String elementType, Locale lang) throws CredentialsNotValidException, UnsupportedEncodingException {

		List<Credentials> toreturn = new ArrayList<Credentials>();

		MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
		params.add("accessToken", accessToken);
		params.add("sourceRequest", "orionbrokerfilter");
		params.add("elementType", elementType);
		// TODO manage the delegation to group nad organization in the cachedcredential

		UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(delegation_endpoint + "/v3/apps/" + URLEncoder.encode(sensorUri, StandardCharsets.UTF_8.toString()) + "/delegator")
				.queryParams(params)
				.build();
		logger.debug("query getDelegatedCredentials {}", uriComponents.toUri());

		// RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

		try {
			ResponseEntity<String> response = restTemplate.exchange(uriComponents.toUri(), HttpMethod.GET, entity, String.class);
			logger.debug("Response from getDelegatedCredentials {}", response);

			ObjectMapper objectMapper = new ObjectMapper();

			if (response.getBody() != null) {// 204 no content body
				JsonNode rootNode = objectMapper.readTree(response.getBody().getBytes());
				Iterator<JsonNode> els = rootNode.elements();

				while ((els != null) && (els.hasNext())) {
					JsonNode elNode = els.next();

					JsonNode udNode = elNode.path("usernameDelegated");
					if ((udNode == null) || (udNode.isNull()) || (udNode.isMissingNode())) {
						logger.error("The retrieved data does not contains userDelegated");
						// throw new CredentialsNotValidException(messages.getMessage("login.ko.configurationerror", null, lang));
						continue;
					}
					if (udNode.asText().equals("ANONYMOUS"))
						cc.setIsPublic(true);

					String ud = udNode.asText();

					JsonNode deNode = elNode.path("delegationDetails");

					if ((deNode == null) || (deNode.isNull()) || (deNode.isMissingNode())) {
						logger.debug("The retrieved data does not contains delegationDetails (probably it's PUBLIC)");
						// throw new CredentialsNotValidException(messages.getMessage("login.ko.configurationerror", null, lang));
						continue;
					}

					logger.debug("delegation details are:" + deNode.asText());

					// delegation details are a string that has to be decoded
					JsonNode deNodeParsed = objectMapper.readTree(deNode.asText());

					JsonNode k1Node = deNodeParsed.path("k1");
					JsonNode k2Node = deNodeParsed.path("k2");

					if ((k1Node == null) || (k1Node.isNull()) || ((k2Node == null) || (k2Node.isNull())) || (k1Node.isMissingNode()) || (k2Node.isMissingNode())) {
						logger.error("The retreived data does not contains k1, k2");
						// throw new CredentialsNotValidException(messages.getMessage("login.ko.configurationerror", null, lang));
						continue;
					}

					logger.debug("k1 is:" + k1Node.asText());
					logger.debug("k2 is:" + k2Node.asText());

					toreturn.add(new Credentials(k1Node.asText(), k2Node.asText(), ud, null));
				}
			}
		} catch (HttpClientErrorException | IOException e) {
			logger.error("Trouble in getDelegatedCredentials", e);
			throw new CredentialsNotValidException(messages.getMessage("login.ko.networkproblems", null, lang));
		}

		cc.setDelegatedCredentials(toreturn);

		return cc;
	}

	private CheckCredential getOwnershipCC(String accessToken, String elementId, String elementType, String username, Locale lang) throws CredentialsNotValidException {

		CheckCredential toreturn = null;

		MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
		params.add("type", elementType);
		params.add("accessToken", accessToken);
		params.add("elementId", elementId);

		String[] elementName = elementId.split(":");

		UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(ownership_endpoint)
				.queryParams(params)
				.build();
		logger.debug("query getOwnerCredentials {}", uriComponents.toUri());

		// RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

		try {
			ResponseEntity<String> response = restTemplate.exchange(uriComponents.toUri(), HttpMethod.GET, entity, String.class);
			logger.debug("Response from  getOwnerCredentials {}", response);

			ObjectMapper objectMapper = new ObjectMapper();

			JsonNode rootNode = objectMapper.readTree(response.getBody().getBytes());
			Iterator<JsonNode> els = rootNode.elements();

			Boolean result = true;

			if ((els == null) || (!els.hasNext())) {
				logger.error("The retrieved data does not contains any elements");
				result = false;
			}

			toreturn = new CheckCredential(elementType, username, result, minutesElapsingCache);

                        if(result && use_blockchain) {
                                JsonNode elNode = els.next();

                                JsonNode edNode = elNode.path("elementDetails");
                                JsonNode certNode = edNode.path("Certified");
                                if ((certNode == null) || (certNode.isNull())  || (certNode.isMissingNode())){
                                        //certifiedDevice.get(elementId).setCertified(false);
                                }else{
                                        logger.info("Device certified");
                                        certifiedDevice.get(elementName[2].replaceAll("\"", "")).setCertified(true);
                                }
                        }
		} catch (HttpClientErrorException | IOException e) {
			logger.error("Trouble in getOwnerCredentials", e);
			throw new CredentialsNotValidException(messages.getMessage("login.ko.networkproblems", null, lang));
		}

		return toreturn;
	}

	private CheckCredential getDelegationCC(String accessToken, String sensorUri, String elementType, String username, Locale lang) throws CredentialsNotValidException, UnsupportedEncodingException {

		CheckCredential toreturn = null;

		MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
		params.add("accessToken", accessToken);
		params.add("sourceRequest", "orionbrokerfilter");
		params.add("elementType", elementType);
		params.add("elementID", sensorUri);

		UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(delegation_endpoint + "/v3/apps/" + URLEncoder.encode(sensorUri, StandardCharsets.UTF_8.toString()) + "/access/check")
				.queryParams(params)
				.build();
		logger.debug("query checkDelegationCredentials {}", uriComponents.toUri());

		// RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

		try {
			ResponseEntity<String> response = restTemplate.exchange(uriComponents.toUri(), HttpMethod.GET, entity, String.class);
			logger.debug("Response from getDelegatedCredentials {}", response);

			ObjectMapper objectMapper = new ObjectMapper();
			Boolean result = false;
			String kind = "READ_ACCESS";

			if (response.getBody() != null) {// 204 no content body
				JsonNode rootNode = objectMapper.readTree(response.getBody().getBytes());

				JsonNode resultNode = rootNode.path("result");
				if ((resultNode == null) || (resultNode.isNull()) || (resultNode.isMissingNode())) {
					logger.error("The retrieved data does not contains resultNode");
					// throw new CredentialsNotValidException(messages.getMessage("login.ko.configurationerror", null, lang));
				} else {

					if (resultNode.asBoolean())
						result = true;

					if (!rootNode.path("kind").isNull()) {
						kind = rootNode.path("kind").asText();
					}

					JsonNode messageNode = rootNode.path("message");

					if ((messageNode == null) || (messageNode.isNull()) || (messageNode.isMissingNode())) {
						logger.debug("The retrieved data does not contains messageNode");
						// throw new CredentialsNotValidException(messages.getMessage("login.ko.configurationerror", null, lang));
					} else if ("PUBLIC".equalsIgnoreCase(messageNode.asText()))
						cachedDelegationPublic.put(sensorUri, new DelegationPublic(elementType, minutesElapsingCache));
				}

			}

			toreturn = new CheckCredential(elementType, username, result, minutesElapsingCache, kind);
		} catch (HttpClientErrorException | IOException e) {
			logger.error("Trouble in getDelegatedCredentials", e);
			throw new CredentialsNotValidException(messages.getMessage("login.ko.networkproblems", null, lang));
		}

		return toreturn;
	}

}