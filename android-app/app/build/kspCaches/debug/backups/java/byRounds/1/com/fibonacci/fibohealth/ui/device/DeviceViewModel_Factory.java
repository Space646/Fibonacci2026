package com.fibonacci.fibohealth.ui.device;

import com.fibonacci.fibohealth.service.BleClient;
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
public final class DeviceViewModel_Factory implements Factory<DeviceViewModel> {
  private final Provider<BleClient> bleClientProvider;

  public DeviceViewModel_Factory(Provider<BleClient> bleClientProvider) {
    this.bleClientProvider = bleClientProvider;
  }

  @Override
  public DeviceViewModel get() {
    return newInstance(bleClientProvider.get());
  }

  public static DeviceViewModel_Factory create(Provider<BleClient> bleClientProvider) {
    return new DeviceViewModel_Factory(bleClientProvider);
  }

  public static DeviceViewModel newInstance(BleClient bleClient) {
    return new DeviceViewModel(bleClient);
  }
}
