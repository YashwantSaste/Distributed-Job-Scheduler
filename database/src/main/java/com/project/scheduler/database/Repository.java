package com.project.scheduler.database;

import java.util.*;

public interface Repository<ID, T> {
  T save(T entity);

  Optional<T> findById(ID id);

  List<T> findAll(int limit, int offset);

  T update(T entity);

  boolean deleteById(ID id);
}
