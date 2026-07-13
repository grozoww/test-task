package io.tempo.json.converter.service.employee.load;

import io.tempo.json.converter.dao.DataSourceDAO;
import io.tempo.json.converter.dto.Employee;
import io.tempo.json.converter.service.employee.map.MapRouterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Loads employees from all configured sources concurrently and combines them into a single
 * de-duplicated list. A source that fails or does not respond within the configured timeout is
 * skipped (with a warning) so one broken source cannot take the whole aggregate down.
 */
@Slf4j
@Service
public class ConcurrentEmployeeDataLoadService implements EmployeeDataLoadService {

    private final MapRouterService<Employee> mapRouterService;
    private final List<DataSourceDAO> dataSources;
    private final Duration loadTimeout;

    public ConcurrentEmployeeDataLoadService(
            MapRouterService<Employee> mapRouterService,
            List<DataSourceDAO> dataSources,
            @Value("${employees.load-timeout:5s}") Duration loadTimeout
    ) {
        this.mapRouterService = mapRouterService;
        this.dataSources = List.copyOf(dataSources);
        this.loadTimeout = loadTimeout;
    }

    @Override
    public List<Employee> getAllEmployees() {
        List<Callable<List<Employee>>> loadTasks = dataSources.stream()
                .map(this::loadTask)
                .toList();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<List<Employee>>> futures =
                    executor.invokeAll(loadTasks, loadTimeout.toMillis(), TimeUnit.MILLISECONDS);
            return combineUnique(futures);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while loading employees", exception);
        }
    }

    private Callable<List<Employee>> loadTask(DataSourceDAO source) {
        return () -> mapRouterService.map(source.getData());
    }

    private List<Employee> combineUnique(List<Future<List<Employee>>> futures) throws InterruptedException {
        Set<Employee> employees = new LinkedHashSet<>();
        for (int i = 0; i < futures.size(); i++) {
            String sourceName = dataSources.get(i).getClass().getSimpleName();
            try {
                employees.addAll(futures.get(i).get());
            } catch (CancellationException exception) {
                log.warn("Skipping source {}: no response within {}", sourceName, loadTimeout);
            } catch (ExecutionException exception) {
                log.warn("Skipping source {}: failed to load or map employees", sourceName, exception.getCause());
            }
        }
        return List.copyOf(employees);
    }
}
