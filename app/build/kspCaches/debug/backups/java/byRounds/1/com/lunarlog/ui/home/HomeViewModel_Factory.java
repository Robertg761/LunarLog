package com.lunarlog.ui.home;

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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<CycleRepository> cycleRepositoryProvider;

  public HomeViewModel_Factory(Provider<CycleRepository> cycleRepositoryProvider) {
    this.cycleRepositoryProvider = cycleRepositoryProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(cycleRepositoryProvider.get());
  }

  public static HomeViewModel_Factory create(Provider<CycleRepository> cycleRepositoryProvider) {
    return new HomeViewModel_Factory(cycleRepositoryProvider);
  }

  public static HomeViewModel newInstance(CycleRepository cycleRepository) {
    return new HomeViewModel(cycleRepository);
  }
}
