package com.fibonacci.fibohealth.service;

import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class BleService_MembersInjector implements MembersInjector<BleService> {
  private final Provider<BleClient> bleClientProvider;

  public BleService_MembersInjector(Provider<BleClient> bleClientProvider) {
    this.bleClientProvider = bleClientProvider;
  }

  public static MembersInjector<BleService> create(Provider<BleClient> bleClientProvider) {
    return new BleService_MembersInjector(bleClientProvider);
  }

  @Override
  public void injectMembers(BleService instance) {
    injectBleClient(instance, bleClientProvider.get());
  }

  @InjectedFieldSignature("com.fibonacci.fibohealth.service.BleService.bleClient")
  public static void injectBleClient(BleService instance, BleClient bleClient) {
    instance.bleClient = bleClient;
  }
}
