package com.fibonacci.fibohealth.service;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class HealthConnectService_Factory implements Factory<HealthConnectService> {
  private final Provider<Context> contextProvider;

  public HealthConnectService_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public HealthConnectService get() {
    return newInstance(contextProvider.get());
  }

  public static HealthConnectService_Factory create(Provider<Context> contextProvider) {
    return new HealthConnectService_Factory(contextProvider);
  }

  public static HealthConnectService newInstance(Context context) {
    return new HealthConnectService(context);
  }
}
