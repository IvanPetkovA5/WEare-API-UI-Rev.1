package restassuredapi.models.models;

import restassuredapi.models.basemodel.BaseModel;

public class CountryModel extends BaseModel {
    private Integer id;
    private String country;

    public Integer getId() {
        return id;
    }

    public String getCountry() {
        return country;
    }
}
