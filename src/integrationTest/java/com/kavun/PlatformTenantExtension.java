package com.kavun;

import com.kavun.shared.util.core.TenantContext;
import java.lang.reflect.Method;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

/**
 * Runs test code as the platform tenant. Tests call services directly, without a signed-in user,
 * which would otherwise be the root context in which no tenant-owned data can be created. Requests
 * sent through MockMvc are not affected: {@link TestConfig} resolves their tenant from the
 * signed-in user, as in production.
 */
public class PlatformTenantExtension implements InvocationInterceptor {

  @Override
  public void interceptBeforeAllMethod(
      Invocation<Void> invocation, ReflectiveInvocationContext<Method> context, ExtensionContext ec)
      throws Throwable {
    asPlatform(invocation);
  }

  @Override
  public void interceptBeforeEachMethod(
      Invocation<Void> invocation, ReflectiveInvocationContext<Method> context, ExtensionContext ec)
      throws Throwable {
    asPlatform(invocation);
  }

  @Override
  public void interceptTestMethod(
      Invocation<Void> invocation, ReflectiveInvocationContext<Method> context, ExtensionContext ec)
      throws Throwable {
    asPlatform(invocation);
  }

  @Override
  public void interceptTestTemplateMethod(
      Invocation<Void> invocation, ReflectiveInvocationContext<Method> context, ExtensionContext ec)
      throws Throwable {
    asPlatform(invocation);
  }

  @Override
  public void interceptAfterEachMethod(
      Invocation<Void> invocation, ReflectiveInvocationContext<Method> context, ExtensionContext ec)
      throws Throwable {
    asPlatform(invocation);
  }

  @Override
  public void interceptAfterAllMethod(
      Invocation<Void> invocation, ReflectiveInvocationContext<Method> context, ExtensionContext ec)
      throws Throwable {
    asPlatform(invocation);
  }

  private static void asPlatform(Invocation<Void> invocation) throws Throwable {
    Throwable[] failure = new Throwable[1];
    TenantContext.runAs(
        TenantContext.PLATFORM,
        () -> {
          try {
            invocation.proceed();
          } catch (Throwable t) {
            failure[0] = t;
          }
        });
    if (failure[0] != null) {
      throw failure[0];
    }
  }
}
