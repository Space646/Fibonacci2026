package com.fibonacci.fibohealth.di;

import com.fibonacci.fibohealth.data.local.FiboHealthDatabase;
import com.fibonacci.fibohealth.data.local.UserProfileDao;
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
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class AppModule_ProvideUserProfileDaoFactory implements Factory<UserProfileDao> {
  private final Provider<FiboHealthDatabase> dbProvider;

  public AppModule_ProvideUserProfileDaoFactory(Provider<FiboHealthDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public UserProfileDao get() {
    return provideUserProfileDao(dbProvider.get());
  }

  public static AppModule_ProvideUserProfileDaoFactory create(
      Provider<FiboHealthDatabase> dbProvider) {
    return new AppModule_ProvideUserProfileDaoFactory(dbProvider);
  }

  public static UserProfileDao provideUserProfileDao(FiboHealthDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideUserProfileDao(db));
  }
}
