/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.rabbitmq.actions;

import static com.rabbitmq.actions.Utils.globPredicate;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class UtilsTest {

  @Test
  void globsPredicateShouldFilterFiles() {
    assertThat(globPredicate("*.rpm,*.dat").test(p("erlang-20.3.8.25-1.el7.x86_64.rpm"))).isTrue();
    assertThat(globPredicate("*.rpm,*.dat").test(p("erlang-20.3.8.25-1.el7.x86_64.txt"))).isFalse();
    assertThat(globPredicate("*.rpm,*.dat").test(p("erlang-20.3.8.25-1.el7.x86_64.dat"))).isTrue();
    assertThat(globPredicate("*.rpm,*.dat").test(p("erlang-20.3.8.25-1.el7.x86_64.bar"))).isFalse();

    assertThat(globPredicate("erlang-*el7*.rpm").test(p("erlang-20.3.8.25-1.el7.x86_64.rpm")))
        .isTrue();
    assertThat(globPredicate("erlang-*20.*el7*.rpm").test(p("erlang-20.3.8.25-1.el7.x86_64.rpm")))
        .isTrue();
    assertThat(globPredicate("erlang-*21.*el7*.rpm").test(p("erlang-20.3.8.25-1.el7.x86_64.rpm")))
        .isFalse();
    assertThat(globPredicate("erlang-*el7*.rpm").test(p("erlang-20.3.8.25-1.el8.x86_64.rpm")))
        .isFalse();
  }

  private static Domain.Package p(String name) {
    Domain.Package p = new Domain.Package();
    p.setFilename(name);
    return p;
  }
}
