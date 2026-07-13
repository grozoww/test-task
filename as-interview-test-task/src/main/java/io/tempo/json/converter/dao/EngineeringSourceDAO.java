package io.tempo.json.converter.dao;

import org.springframework.stereotype.Component;

@Component
public class EngineeringSourceDAO implements DataSourceDAO {

    private static final String DATA = """
            {
              "users": {
                "U-8899": {
                  "displayName": "Michael Scott",
                  "mainEmail": "mscott@paper.biz",
                  "metadata": {
                    "custom_attributes": [
                      { "key": "role", "value": "Senior Java Developer" },
                      { "key": "location", "value": "Scranton" }
                    ],
                    "account_suspended": false
                  }
                },
                "U-9900": {
                  "displayName": "Pam Beesly",
                  "mainEmail": "pam@paper.biz",
                  "metadata": {
                    "custom_attributes": [
                      { "key": "role", "value": "Scala Engineer" },
                      { "key": "location", "value": "Scranton" }
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
