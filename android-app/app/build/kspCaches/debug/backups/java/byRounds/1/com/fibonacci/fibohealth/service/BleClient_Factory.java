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
public final class BleClient_Factory implements Factory<BleClient> {
  private final Provider<Context> contextProvider;

  public BleClient_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public BleClient get() {
    return newInstance(contextProvider.get());
  }

  public static BleClient_Factory create(Provider<Context> contextProvider) {
    return new BleClient_Factory(contextProvider);
  }

  public static BleClient newInstance(Context context) {
    return new BleClient(context);
  }
}
