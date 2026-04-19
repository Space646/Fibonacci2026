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
public final class HealthConnectFoodLogger_Factory implements Factory<HealthConnectFoodLogger> {
  private final Provider<Context> contextProvider;

  private final Provider<HealthConnectService> hcServiceProvider;

  public HealthConnectFoodLogger_Factory(Provider<Context> contextProvider,
      Provider<HealthConnectService> hcServiceProvider) {
    this.contextProvider = contextProvider;
    this.hcServiceProvider = hcServiceProvider;
  }

  @Override
  public HealthConnectFoodLogger get() {
    return newInstance(contextProvider.get(), hcServiceProvider.get());
  }

  public static HealthConnectFoodLogger_Factory create(Provider<Context> contextProvider,
      Provider<HealthConnectService> hcServiceProvider) {
    return new HealthConnectFoodLogger_Factory(contextProvider, hcServiceProvider);
  }

  public static HealthConnectFoodLogger newInstance(Context context,
      HealthConnectService hcService) {
    return new HealthConnectFoodLogger(context, hcService);
  }
}
