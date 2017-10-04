/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.utils;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TimeProfilerTest {

  private Logger logger;

  @Before
  public void before() {
    logger = mock(Logger.class);
  }

  @Test
  public void testBasicProfiling() {
    TimeProfiler profiler = new TimeProfiler(logger);
    profiler.start("Cycle analysis");
    verify(logger).info("Cycle analysis...");

    profiler.stop();
    verify(logger).info(eq("{} done: {} ms"), eq("Cycle analysis"), anyLong());
  }

  @Test
  public void stopOnce() {
    TimeProfiler profiler = new TimeProfiler(logger);

    profiler.start("Cycle analysis");
    profiler.stop();
    profiler.stop();
    profiler.stop();
    verify(logger, times(1)).info(anyString()); // start() executes log() with 1 parameter
    verify(logger, times(1)).info(anyString(), anyString(), anyLong()); // stop() executes log() with 3 parameters
  }

  @Test
  public void doNotLogNeverEndedTask() {
    TimeProfiler profiler = new TimeProfiler(logger);

    profiler.start("Cycle analysis");
    profiler.start("New task");
    profiler.stop();
    profiler.stop();
    verify(logger, never()).info(eq("{} done: {} ms"), eq("Cycle analysis"), any());
    verify(logger, times(1)).info(eq("{} done: {} ms"), eq("New task"), anyLong());
  }
}
