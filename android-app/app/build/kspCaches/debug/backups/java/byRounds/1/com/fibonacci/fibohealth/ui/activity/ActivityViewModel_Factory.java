package com.fibonacci.fibohealth.ui.activity;

import com.fibonacci.fibohealth.data.repository.ProfileRepository;
import com.fibonacci.fibohealth.service.BleClient;
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
public final class ActivityViewModel_Factory implements Factory<ActivityViewModel> {
  private final Provider<HealthConnectService> hcServiceProvider;

  private final Provider<BleClient> bleClientProvider;

  private final Provider<ProfileRepository> profileRepoProvider;

  public ActivityViewModel_Factory(Provider<HealthConnectService> hcServiceProvider,
      Provider<BleClient> bleClientProvider, Provider<ProfileRepository> profileRepoProvider) {
    this.hcServiceProvider = hcServiceProvider;
    this.bleClientProvider = bleClientProvider;
    this.profileRepoProvider = profileRepoProvider;
  }

  @Override
  public ActivityViewModel get() {
    return newInstance(hcServiceProvider.get(), bleClientProvider.get(), profileRepoProvider.get());
  }

  public static ActivityViewModel_Factory create(Provider<HealthConnectService> hcServiceProvider,
      Provider<BleClient> bleClientProvider, Provider<ProfileRepository> profileRepoProvider) {
    return new ActivityViewModel_Factory(hcServiceProvider, bleClientProvider, profileRepoProvider);
  }

  public static ActivityViewModel newInstance(HealthConnectService hcService, BleClient bleClient,
      ProfileRepository profileRepo) {
    return new ActivityViewModel(hcService, bleClient, profileRepo);
  }
}
