package com.lunarlog.di;

import com.lunarlog.data.AppDatabase;
import com.lunarlog.data.CycleDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideCycleDaoFactory implements Factory<CycleDao> {
  private final Provider<AppDatabase> databaseProvider;

  public AppModule_ProvideCycleDaoFactory(Provider<AppDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public CycleDao get() {
    return provideCycleDao(databaseProvider.get());
  }

  public static AppModule_ProvideCycleDaoFactory create(Provider<AppDatabase> databaseProvider) {
    return new AppModule_ProvideCycleDaoFactory(databaseProvider);
  }

  public static CycleDao provideCycleDao(AppDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideCycleDao(database));
  }
}
