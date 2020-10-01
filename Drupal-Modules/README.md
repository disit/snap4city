# Setup instructions

* install drupal 7
* install modules:
  * Simple LDAP (7.x-1.4) (enable Role and User submodules)
  * OpenID Connect (7.x-1.0-beta8+2-dev)
  * OpenIDConnectPatch
  * Organic Groups (7.x-2.10)
  * OrganicGroupSimpleLdap
  * ProfileSnap4City
* Setup drupal:
  * add Content type "Organization" with machine name "disitorg" and set it as Group in the Organic groups section
  * add Content type "Organization group" with machine name "disitgroup" and set it as Group and Group content"
  * ...TBD...
