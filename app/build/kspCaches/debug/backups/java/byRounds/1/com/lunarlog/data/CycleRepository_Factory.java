package com.lunarlog.data;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class CycleRepository_Factory implements Factory<CycleRepository> {
  private final Provider<CycleDao> cycleDaoProvider;

  public CycleRepository_Factory(Provider<CycleDao> cycleDaoProvider) {
    this.cycleDaoProvider = cycleDaoProvider;
  }

  @Override
  public CycleRepository get() {
    return newInstance(cycleDaoProvider.get());
  }

  public static CycleRepository_Factory create(Provider<CycleDao> cycleDaoProvider) {
    return new CycleRepository_Factory(cycleDaoProvider);
  }

  public static CycleRepository newInstance(CycleDao cycleDao) {
    return new CycleRepository(cycleDao);
  }
}
