package com.fibonacci.fibohealth.data.repository;

import com.fibonacci.fibohealth.data.local.UserProfileDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class ProfileRepository_Factory implements Factory<ProfileRepository> {
  private final Provider<UserProfileDao> daoProvider;

  public ProfileRepository_Factory(Provider<UserProfileDao> daoProvider) {
    this.daoProvider = daoProvider;
  }

  @Override
  public ProfileRepository get() {
    return newInstance(daoProvider.get());
  }

  public static ProfileRepository_Factory create(Provider<UserProfileDao> daoProvider) {
    return new ProfileRepository_Factory(daoProvider);
  }

  public static ProfileRepository newInstance(UserProfileDao dao) {
    return new ProfileRepository(dao);
  }
}
