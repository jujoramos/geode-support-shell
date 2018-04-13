package org.apache.geode.support.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.junit.Test;

public class FormatUtilsTest {

  @Test
  public void relativizePathTest() {
    Path mockedFile1 = mock(Path.class);
    Path mockedFile2 = mock(Path.class);
    when(mockedFile1.toAbsolutePath()).thenReturn(mock(Path.class));
    when(mockedFile2.toAbsolutePath()).thenReturn(mock(Path.class));

    when(mockedFile1.toAbsolutePath().toString()).thenReturn("/Users/root");
    when(mockedFile2.toAbsolutePath().toString()).thenReturn("/Users/bruceWayne/file.txt");
    assertThat(FormatUtils.relativizePath(mockedFile1, mockedFile2)).isEqualTo("/Users/bruceWayne/file.txt");

    when(mockedFile1.toAbsolutePath().toString()).thenReturn("/");
    when(mockedFile2.toAbsolutePath().toString()).thenReturn("/file.txt");
    assertThat(FormatUtils.relativizePath(mockedFile1, mockedFile2)).isEqualTo("file.txt");

    when(mockedFile1.toAbsolutePath().toString()).thenReturn("file.txt");
    when(mockedFile2.toAbsolutePath().toString()).thenReturn("file.txt");
    assertThat(FormatUtils.relativizePath(mockedFile1, mockedFile2)).isEqualTo("file.txt");

    when(mockedFile1.toAbsolutePath().toString()).thenReturn("/Users/root/data/files/myFile.dat");
    when(mockedFile2.toAbsolutePath().toString()).thenReturn("myFile.dat");
    assertThat(FormatUtils.relativizePath(mockedFile1, mockedFile2)).isEqualTo("myFile.dat");
  }

  @Test
  public void getDateTimeFormatterTest() {
    DateTimeFormatter formatter = FormatUtils.getDateTimeFormatter();
    assertThat(formatter).isNotNull();
    assertThat(formatter.getLocale()).isEqualTo(Locale.getDefault());
  }

  @Test
  public void trimProductVersionTest() {
    assertThat(FormatUtils.trimProductVersion("")).isEqualTo("");
    assertThat(FormatUtils.trimProductVersion("  Geode 1.3.0   ")).isEqualTo("Geode 1.3.0");
    assertThat(FormatUtils.trimProductVersion("GemFire 1.4.0 #jramos 0 as of 2018-01-26 08:34:31 -0800")).isEqualTo("GemFire 1.4.0");
    assertThat(FormatUtils.trimProductVersion("GemFire 9.3.0 #jramos 0 # as of 2018-01-26 08:34:31 -0800")).isEqualTo("GemFire 9.3.0");
  }

  @Test
  public void formatTimeZoneIdTest() {
    assertThat(FormatUtils.formatTimeZoneId(null)).isEqualTo("");
    assertThat(FormatUtils.formatTimeZoneId(ZoneId.of("Europe/Paris"))).isEqualTo("[Europe/Paris]");
    assertThat(FormatUtils.formatTimeZoneId(ZoneId.of("America/Chicago"))).isEqualTo("[America/Chicago]");
    assertThat(FormatUtils.formatTimeZoneId(ZoneId.of("America/Argentina/Buenos_Aires"))).isEqualTo("[America/Argentina/Buenos_Aires]");

  }
}
