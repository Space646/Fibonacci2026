package com.fibonacci.fibohealth.ui.dashboard;

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
public final class DashboardViewModel_Factory implements Factory<DashboardViewModel> {
  private final Provider<ProfileRepository> profileRepoProvider;

  private final Provider<BleClient> bleClientProvider;

  private final Provider<HealthConnectService> hcServiceProvider;

  public DashboardViewModel_Factory(Provider<ProfileRepository> profileRepoProvider,
      Provider<BleClient> bleClientProvider, Provider<HealthConnectService> hcServiceProvider) {
    this.profileRepoProvider = profileRepoProvider;
    this.bleClientProvider = bleClientProvider;
    this.hcServiceProvider = hcServiceProvider;
  }

  @Override
  public DashboardViewModel get() {
    return newInstance(profileRepoProvider.get(), bleClientProvider.get(), hcServiceProvider.get());
  }

  public static DashboardViewModel_Factory create(Provider<ProfileRepository> profileRepoProvider,
      Provider<BleClient> bleClientProvider, Provider<HealthConnectService> hcServiceProvider) {
    return new DashboardViewModel_Factory(profileRepoProvider, bleClientProvider, hcServiceProvider);
  }

  public static DashboardViewModel newInstance(ProfileRepository profileRepo, BleClient bleClient,
      HealthConnectService hcService) {
    return new DashboardViewModel(profileRepo, bleClient, hcService);
  }
}
