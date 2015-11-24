/*
 * Copyright 2012-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.android;

import static com.facebook.buck.rules.BuildableProperties.Kind.ANDROID;

import com.facebook.buck.rules.AddToRuleKey;
import com.facebook.buck.rules.BuildRule;
import com.facebook.buck.rules.BuildRuleParams;
import com.facebook.buck.rules.BuildableProperties;
import com.facebook.buck.rules.ExopackageInfo;
import com.facebook.buck.rules.InstallableApk;
import com.facebook.buck.rules.SourcePath;
import com.facebook.buck.rules.SourcePathResolver;
import com.facebook.buck.rules.args.Arg;
import com.facebook.buck.shell.Genrule;
import com.facebook.buck.step.ExecutionContext;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import java.nio.file.Path;
import java.util.List;

/**
 * A specialization of a genrule that specifically allows the modification of apks.  This is
 * useful for processes that modify an APK, such as zipaligning it or signing it.
 * <p>
 * The generated APK will be at <code><em>rule_name</em>.apk</code>.
 * <pre>
 * apk_genrule(
 *   name = 'fb4a_signed',
 *   apk = ':fbandroid_release'
 *   deps = [
 *     '//java/com/facebook/sign:fbsign_jar',
 *   ],
 *   cmd = '${//java/com/facebook/sign:fbsign_jar} --input $APK --output $OUT'
 * )
 * </pre>
 */
public class ApkGenrule extends Genrule implements InstallableApk {

  private static final BuildableProperties PROPERTIES = new BuildableProperties(ANDROID);
  @AddToRuleKey
  private final SourcePath apk;

  ApkGenrule(
      BuildRuleParams params,
      SourcePathResolver resolver,
      List<SourcePath> srcs,
      Optional<Arg> cmd,
      Optional<Arg> bash,
      Optional<Arg> cmdExe,
      SourcePath apk) {
    super(
        params,
        resolver,
        srcs,
        cmd,
        bash,
        cmdExe,
        /* out */ params.getBuildTarget().getShortNameAndFlavorPostfix() + ".apk");

    Optional<BuildRule> rule = resolver.getRule(apk);
    Preconditions.checkState(rule.isPresent());
    Preconditions.checkState(rule.get() instanceof InstallableApk);

    this.apk = apk;
  }

  @Override
  public BuildableProperties getProperties() {
    return PROPERTIES;
  }

  public InstallableApk getInstallableApk() {
    return (InstallableApk) getResolver().getRule(apk).get();
  }

  @Override
  public Path getManifestPath() {
    return getInstallableApk().getManifestPath();
  }

  @Override
  public Path getApkPath() {
    return getPathToOutput();
  }

  @Override
  public Optional<ExopackageInfo> getExopackageInfo() {
    return getInstallableApk().getExopackageInfo();
  }

  @Override
  protected void addEnvironmentVariables(
      ExecutionContext context,
      ImmutableMap.Builder<String, String> environmentVariablesBuilder) {
    super.addEnvironmentVariables(context, environmentVariablesBuilder);
    // We have to use an absolute path, because genrules are run in a temp directory.
    InstallableApk installApk = getInstallableApk();
    String apkAbsolutePath = installApk.getProjectFilesystem()
        .resolve(installApk.getApkPath())
        .toString();
    environmentVariablesBuilder.put("APK", apkAbsolutePath);
  }
}
