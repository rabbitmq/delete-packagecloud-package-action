/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.rabbitmq.actions;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;

public class JsonTest {

  private static final String JSON =
      "{\n"
          + "      \"name\":\"erlang\",\n"
          + "      \"distro_version\":\"el/7\",\n"
          + "      \"created_at\":\"2020-01-24T05:35:34.000Z\",\n"
          + "      \"version\":\"20.3.8.25\",\n"
          + "      \"release\":\"1.el7\",\n"
          + "      \"epoch\":0,\n"
          + "      \"scope\":null,\n"
          + "      \"private\":false,\n"
          + "      \"type\":\"rpm\",\n"
          + "      \"filename\":\"erlang-20.3.8.25-1.el7.x86_64.rpm\",\n"
          + "      \"uploader_name\":\"rabbitmq\",\n"
          + "      \"indexed\":true,\n"
          + "      \"sha256sum\":\"09c2b01168517a7992aab81c6eeaad4be08396b97315b41866f60832ff165de4\",\n"
          + "      \"repository_html_url\":\"/rabbitmq/erlang\",\n"
          + "      \"package_url\":\"/api/v1/repos/rabbitmq/erlang/package/rpm/el/7/erlang/x86_64/20.3.8.25/1.el7.json\",\n"
          + "      \"downloads_detail_url\":\"/api/v1/repos/rabbitmq/erlang/package/rpm/el/7/erlang/x86_64/20.3.8.25/1.el7/stats/downloads/detail.json\",\n"
          + "      \"downloads_series_url\":\"/api/v1/repos/rabbitmq/erlang/package/rpm/el/7/erlang/x86_64/20.3.8.25/1.el7/stats/downloads/series/daily.json\",\n"
          + "      \"downloads_count_url\":\"/api/v1/repos/rabbitmq/erlang/package/rpm/el/7/erlang/x86_64/20.3.8.25/1.el7/stats/downloads/count.json\",\n"
          + "      \"package_html_url\":\"/rabbitmq/erlang/packages/el/7/erlang-20.3.8.25-1.el7.x86_64.rpm\",\n"
          + "      \"download_url\":\"https://packagecloud.io/rabbitmq/erlang/packages/el/7/erlang-20.3.8.25-1.el7.x86_64.rpm/download.rpm?distro_version_id=140\",\n"
          + "      \"promote_url\":\"/api/v1/repos/rabbitmq/erlang/el/7/erlang-20.3.8.25-1.el7.x86_64.rpm/promote.json\",\n"
          + "      \"destroy_url\":\"/api/v1/repos/rabbitmq/erlang/el/7/erlang-20.3.8.25-1.el7.x86_64.rpm\"\n"
          + "   }";

  @Test
  void jsonPackageShouldDeserialized() {
    Domain.Package p = Utils.GSON.fromJson(JSON, Domain.Package.class);
    assertThat(p.filename()).isEqualTo("erlang-20.3.8.25-1.el7.x86_64.rpm");
    assertThat(p.destroy_url())
        .isEqualTo("/api/v1/repos/rabbitmq/erlang/el/7/erlang-20.3.8.25-1.el7.x86_64.rpm");
    assertThat(p.version()).isEqualTo("20.3.8.25");
    assertThat(p.created_at())
        .isEqualTo(
            ZonedDateTime.parse("2020-01-24T05:35:34.000Z", DateTimeFormatter.ISO_ZONED_DATE_TIME));
  }
}
