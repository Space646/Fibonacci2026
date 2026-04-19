package com.fibonacci.fibohealth.ui.profile;

import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import com.fibonacci.fibohealth.data.repository.ProfileRepository;
import com.fibonacci.fibohealth.service.BleClient;
import com.fibonacci.fibohealth.service.HealthConnectFoodLogger;
import com.fibonacci.fibohealth.service.HealthConnectService;
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
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class ProfileViewModel_Factory implements Factory<ProfileViewModel> {
  private final Provider<ProfileRepository> profileRepoProvider;

  private final Provider<BleClient> bleClientProvider;

  private final Provider<HealthConnectService> hcServiceProvider;

  private final Provider<HealthConnectFoodLogger> hcLoggerProvider;

  private final Provider<DataStore<Preferences>> dataStoreProvider;

  public ProfileViewModel_Factory(Provider<ProfileRepository> profileRepoProvider,
      Provider<BleClient> bleClientProvider, Provider<HealthConnectService> hcServiceProvider,
      Provider<HealthConnectFoodLogger> hcLoggerProvider,
      Provider<DataStore<Preferences>> dataStoreProvider) {
    this.profileRepoProvider = profileRepoProvider;
    this.bleClientProvider = bleClientProvider;
    this.hcServiceProvider = hcServiceProvider;
    this.hcLoggerProvider = hcLoggerProvider;
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public ProfileViewModel get() {
    return newInstance(profileRepoProvider.get(), bleClientProvider.get(), hcServiceProvider.get(), hcLoggerProvider.get(), dataStoreProvider.get());
  }

  public static ProfileViewModel_Factory create(Provider<ProfileRepository> profileRepoProvider,
      Provider<BleClient> bleClientProvider, Provider<HealthConnectService> hcServiceProvider,
      Provider<HealthConnectFoodLogger> hcLoggerProvider,
      Provider<DataStore<Preferences>> dataStoreProvider) {
    return new ProfileViewModel_Factory(profileRepoProvider, bleClientProvider, hcServiceProvider, hcLoggerProvider, dataStoreProvider);
  }

  public static ProfileViewModel newInstance(ProfileRepository profileRepo, BleClient bleClient,
      HealthConnectService hcService, HealthConnectFoodLogger hcLogger,
      DataStore<Preferences> dataStore) {
    return new ProfileViewModel(profileRepo, bleClient, hcService, hcLogger, dataStore);
  }
}
