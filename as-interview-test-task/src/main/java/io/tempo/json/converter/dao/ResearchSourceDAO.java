package io.tempo.json.converter.dao;

import org.springframework.stereotype.Component;

@Component
public class ResearchSourceDAO implements DataSourceDAO {

    private static final String DATA = """
            {
              "results": [
                {
                  "employeeNumber": "W-2001",
                  "personal": { "fullName": "Grace Hopper" },
                  "work": { "emailAddress": "ghopper@navy.mil", "position": "Rear Admiral" },
                  "employmentStatus": "ACTIVE"
                },
                {
                  "employeeNumber": "W-2002",
                  "personal": { "fullName": "Katherine Johnson" },
                  "work": { "emailAddress": "kjohnson@navy.mil", "position": "Mathematician" },
                  "employmentStatus": "TERMINATED"
                }
              ]
            }""";

    @Override
    public String getData() {
        return DATA;
    }
}
