package org.maxwe.tao.server.controller.account.model;

import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.annotation.JSONField;

/**
 * Created by Pengwei Ding on 2017-01-09 19:08.
 * Email: www.dingpengwei@foxmail.com www.dingpegnwei@gmail.com
 * Description:
 */
public class ExistModel extends TokenModel {
    private String cellphone;

    public ExistModel() {
        super();
    }

    public ExistModel(String cellphone) {
        this.cellphone = cellphone;
    }

    public String getCellphone() {
        return cellphone;
    }

    public void setCellphone(String cellphone) {
        this.cellphone = cellphone;
    }

    @Override
    public String toString() {
        return "ExistModel{" +
                "cellphone='" + cellphone + '\'' +
                '}';
    }

    @JSONField(serialize = false)
    public boolean isParamsOk() {
        if (!StringUtils.isEmpty(this.getCellphone())) {
            return true;
        }
        return false;
    }
}
