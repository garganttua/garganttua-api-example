package com.garganttua.api.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.garganttua.api.commons.ApiException;
import com.garganttua.api.commons.dao.IDao;
import com.garganttua.api.commons.definition.IDomainDefinition;
import com.garganttua.api.commons.filter.IFilter;
import com.garganttua.api.commons.pageable.IPageable;
import com.garganttua.api.commons.sort.ISort;
import com.garganttua.core.reflection.annotations.Reflected;

@Reflected
public class InMemoryDao implements IDao {

    private final List<Object> storage = new ArrayList<>();

    @Override
    public void registerDomain(IDomainDefinition domainDefinition) {
    }

    @Override
    public List<Object> find(Optional<IPageable> pageable, Optional<IFilter> filter, Optional<ISort> sort)
            throws ApiException {
        if (filter.isPresent()) {
            return filterStorage(filter.get());
        }
        return new ArrayList<>(storage);
    }

    @Override
    public Object save(Object object) throws ApiException {
        storage.add(object);
        return object;
    }

    @Override
    public void delete(Object object) throws ApiException {
        String uuid = extractUuid(object);
        if (uuid != null) {
            storage.removeIf(stored -> uuid.equals(extractUuid(stored)));
        } else {
            storage.remove(object);
        }
    }

    @Override
    public long count(IFilter filter) throws ApiException {
        if (filter == null) return storage.size();
        return filterStorage(filter).size();
    }

    public List<Object> getStorage() {
        return storage;
    }

    private List<Object> filterStorage(IFilter f) {
        if ("$and".equals(f.getName()) && f.getFilters() != null) {
            List<Object> result = new ArrayList<>(storage);
            for (IFilter sub : f.getFilters()) {
                result = filterList(result, sub);
            }
            return result;
        }
        return filterList(new ArrayList<>(storage), f);
    }

    private List<Object> filterList(List<Object> list, IFilter f) {
        if ("$field".equals(f.getName()) && f.getFilters() != null) {
            String fieldName = String.valueOf(f.getValue());
            IFilter operator = f.getFilters().get(0);
            if ("$eq".equals(operator.getName())) {
                Object expected = operator.getValue();
                List<Object> result = new ArrayList<>();
                for (Object obj : list) {
                    try {
                        java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
                        field.setAccessible(true);
                        Object actual = field.get(obj);
                        if (expected != null && expected.equals(actual)) {
                            result.add(obj);
                        }
                    } catch (Exception e) {
                        // skip
                    }
                }
                return result;
            }
        }
        return list;
    }

    private String extractUuid(Object obj) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField("uuid");
            field.setAccessible(true);
            Object value = field.get(obj);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
