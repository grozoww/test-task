package io.tempo.json.converter.dao;

import org.springframework.stereotype.Component;

@Component
public class SecuritySourceDAO implements DataSourceDAO {

    private static final String DATA = """
            {
              "data": {
                "employees": [
                  {
                    "_id": "e_554",
                    "profile": {
                      "name": {
                        "first": "Sarah",
                        "last": "Connor"
                      },
                      "age": 34
                    },
                    "contact": {
                      "workEmail": "s.connor@tech.io",
                      "phone": "555-0199"
                    },
                    "attributes": {
                      "department": "Security",
                      "role": "Chief Officer",
                      "status": "active"
                    }
                  },
                  {
                    "_id": "e_555",
                    "profile": {
                      "name": {
                        "first": "John",
                        "last": "Doe"
                      },
                      "age": 29
                    },
                    "contact": {
                      "workEmail": "j.doe@tech.io",
                      "phone": "555-0200"
                    },
                    "attributes": {
                      "department": "Engineering",
                      "role": "Junior Dev",
                      "status": "on_leave"
                    }
                  }
                ]
              }
            }""";

    @Override
    public String getData() {
        return DATA;
    }
}
