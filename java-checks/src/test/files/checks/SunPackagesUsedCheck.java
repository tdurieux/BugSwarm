import com.sun.imageio.plugins; // Noncompliant {{Replace this usage of Sun classes by ones from the Java API.}}
import com.sun.jersey.api.client.ClientHandlerException; // Noncompliant
import java.util.ArrayList;

class A {
  private void f() {
    com.sun.imageio.plugins.bmp a = new com.sun.imageio.plugins.bmp(); // Noncompliant
    new com.sun.imageio.plugins.bmp(); // Noncompliant
    java.util.List a;
    sun.Foo a; // Noncompliant
    db.setErrorHandler(new com.sun.org.apache.xml.internal.security.utils // Noncompliant
        .IgnoreAllErrorHandler());
    sun       // Noncompliant
        .Foo.toto
        .asd a;

    new Foo<com.sun.Bar>() {}; // Noncompliant
  }
}
