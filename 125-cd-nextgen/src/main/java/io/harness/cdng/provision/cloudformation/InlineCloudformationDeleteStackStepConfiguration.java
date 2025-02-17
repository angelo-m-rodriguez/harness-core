/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.provision.cloudformation;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;
import io.harness.validation.Validator;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CDP)
@RecasterAlias("io.harness.cdng.provision.cloudformation.InlineCloudformationDeleteStackStepConfiguration")
public class InlineCloudformationDeleteStackStepConfiguration
    implements CloudformationDeleteStackStepConfigurationSpec {
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> stackName;
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> connectorRef;
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> region;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> roleArn;
  void validateParams() {
    Validator.notNullCheck("AWS connectorRef is null", connectorRef);
    Validator.notNullCheck("AWS region is null", region);
    Validator.notNullCheck("StackName is null", stackName);
  }

  @Override
  public String getType() {
    return CloudformationDeleteStackStepConfigurationTypes.Inline;
  }
}
