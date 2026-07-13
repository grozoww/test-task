package io.tempo.json.converter.dao;

import org.springframework.stereotype.Component;

@Component
public class DesignSourceDAO implements DataSourceDAO {

    private static final String DATA = """
            {
              "users": {
                "U-9914": {
                  "displayName": "Alex Montana",
                  "mainEmail": "amontana@paper.biz",
                  "metadata": {
                    "custom_attributes": [
                      { "key": "role", "value": "Designer" },
                      { "key": "location", "value": "New-York" }
                    ],
                    "account_suspended": false
                  }
                },
                "U-9999": {
                  "displayName": "Mary McLaren",
                  "mainEmail": "mmclaren@paper.biz",
                  "metadata": {
                    "custom_attributes": [
                      { "key": "role", "value": "UX Designer" },
                      { "key": "location", "value": "London" }
                    ],
                    "account_suspended": true
                  }
                }
              }
            }""";

    @Override
    public String getData() {
        return DATA;
    }
}
