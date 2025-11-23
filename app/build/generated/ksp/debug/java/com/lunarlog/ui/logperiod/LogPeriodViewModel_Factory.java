package com.lunarlog.ui.logperiod;

import com.lunarlog.data.CycleRepository;
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
public final class LogPeriodViewModel_Factory implements Factory<LogPeriodViewModel> {
  private final Provider<CycleRepository> cycleRepositoryProvider;

  public LogPeriodViewModel_Factory(Provider<CycleRepository> cycleRepositoryProvider) {
    this.cycleRepositoryProvider = cycleRepositoryProvider;
  }

  @Override
  public LogPeriodViewModel get() {
    return newInstance(cycleRepositoryProvider.get());
  }

  public static LogPeriodViewModel_Factory create(
      Provider<CycleRepository> cycleRepositoryProvider) {
    return new LogPeriodViewModel_Factory(cycleRepositoryProvider);
  }

  public static LogPeriodViewModel newInstance(CycleRepository cycleRepository) {
    return new LogPeriodViewModel(cycleRepository);
  }
}
