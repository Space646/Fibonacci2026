package com.fibonacci.fibohealth.ui.foodlog;

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
public final class FoodLogViewModel_Factory implements Factory<FoodLogViewModel> {
  private final Provider<BleClient> bleClientProvider;

  public FoodLogViewModel_Factory(Provider<BleClient> bleClientProvider) {
    this.bleClientProvider = bleClientProvider;
  }

  @Override
  public FoodLogViewModel get() {
    return newInstance(bleClientProvider.get());
  }

  public static FoodLogViewModel_Factory create(Provider<BleClient> bleClientProvider) {
    return new FoodLogViewModel_Factory(bleClientProvider);
  }

  public static FoodLogViewModel newInstance(BleClient bleClient) {
    return new FoodLogViewModel(bleClient);
  }
}
