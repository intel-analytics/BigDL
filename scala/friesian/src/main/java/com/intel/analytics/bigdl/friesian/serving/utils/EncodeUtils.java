/*
 * Copyright 2021 The BigDL Authors.
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

package com.intel.analytics.bigdl.friesian.serving.utils;

import org.apache.commons.io.serialization.ValidatingObjectInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.regex.Pattern;


public class EncodeUtils {
    private static final Logger logger = LogManager.getLogger(EncodeUtils.class.getName());
    public static byte[] objToBytes(Object o) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(o);
            out.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            logger.warn(e.getMessage());
        } finally {
            try {
                out.close();
                bos.close();
            } catch (IOException e) {
                logger.warn(e.getMessage());
            }
        }
        return new byte[0];
    }

    public static Object bytesToObj(byte[] bytes) {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ValidatingObjectInputStream in = null;
        try {
            in = new ValidatingObjectInputStream(bis);
            Pattern compile = Pattern.compile("java.*");
            Pattern compile1 = Pattern.compile("org.apache.*");
            Pattern compile2 = Pattern.compile("scala.*");
            Pattern compile3 = Pattern.compile("com.intel.analytics.bigdl.*");
            in.accept(compile.pattern(), compile1.pattern(), compile2.pattern(), compile3.pattern());
            in.accept("[*");
            return in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            logger.warn(e.getMessage());
        } finally {
            try {
                in.close();
                bis.close();
            } catch (IOException e) {
                logger.warn(e.getMessage());
            }
        }
        return null;
    }
}
