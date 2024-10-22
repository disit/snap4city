package edu.unifi.disit.orionbrokerfilter.datamodel;





public class Certified {
    private Boolean isCertified=false;
    private String bearerToken=null;

    //TODO settare il device type nella certificazione
    private String deviceType=null;
    private String organization=null;

    public Certified(Boolean isCertified, String bearerToken, String deviceType, String organization) {
        this.isCertified = isCertified;
        this.bearerToken = bearerToken;
        this.deviceType = deviceType;
        this.organization = organization;
    }

    public Certified() {
    }

    public Boolean getCertified() {
        return this.isCertified;
    }

    public void setCertified(Boolean certified) {
        this.isCertified = certified;
    }


    public String getBearerToken() {
        return this.bearerToken;
    }

    public void setBearerToken( String bearerToken) {
        this.bearerToken = bearerToken;
    }


    public String getDeviceType() {
        return this.deviceType;
    }

    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public String getOrganization() {return organization;}

    public void setOrganization(String organization) {this.organization = organization;}
}
