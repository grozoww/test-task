package io.tempo.json.converter.service.employee.map;

import java.util.List;

public interface MapRouterService<T> {

    List<T> map(String json);
}
