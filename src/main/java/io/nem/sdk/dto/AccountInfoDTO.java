/*
 * Copyright 2019 NEM
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.nem.sdk.dto;

import com.google.gson.annotations.SerializedName;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;

/**
 * AccountInfoDTO
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2019-02-28T19:50:06.335-03:00")
public class AccountInfoDTO {
  @SerializedName("meta")
  private AccountMetaDTO meta = null;

  @SerializedName("account")
  private AccountDTO account = null;

  public AccountInfoDTO meta(AccountMetaDTO meta) {
    this.meta = meta;
    return this;
  }

   /**
   * Get meta
   * @return meta
  **/
  @ApiModelProperty(required = true, value = "")
  public AccountMetaDTO getMeta() {
    return meta;
  }

  public void setMeta(AccountMetaDTO meta) {
    this.meta = meta;
  }

  public AccountInfoDTO account(AccountDTO account) {
    this.account = account;
    return this;
  }

   /**
   * Get account
   * @return account
  **/
  @ApiModelProperty(required = true, value = "")
  public AccountDTO getAccount() {
    return account;
  }

  public void setAccount(AccountDTO account) {
    this.account = account;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AccountInfoDTO accountInfoDTO = (AccountInfoDTO) o;
    return Objects.equals(this.meta, accountInfoDTO.meta) &&
        Objects.equals(this.account, accountInfoDTO.account);
  }

  @Override
  public int hashCode() {
    return Objects.hash(meta, account);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AccountInfoDTO {\n");
    
    sb.append("    meta: ").append(toIndentedString(meta)).append("\n");
    sb.append("    account: ").append(toIndentedString(account)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}

