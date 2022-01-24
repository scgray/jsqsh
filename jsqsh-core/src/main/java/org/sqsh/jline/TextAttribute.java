/*
 * Copyright 2007-2022 Scott C. Gray
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
package org.sqsh.jline;

import org.jline.utils.AttributedStringBuilder;

public class TextAttribute {

    public static TextAttribute INSTANCE = new TextAttribute();
    public static TextAttribute FOREGROUND = INSTANCE;
    public static BackgroundTextAttribute BACKGROUND = new BackgroundTextAttribute();

    protected AttributedStringBuilder sb = new AttributedStringBuilder();

    private TextAttribute() {

    }

    public TextAttribute getForeground() {

        return FOREGROUND;
    }

    public BackgroundTextAttribute getBackground() {

        return BACKGROUND;
    }

    public String getDefault() {

        return "\033[0m\033[39m\033[49m";
    }

    public String getBold() {

        return "\033[1m";
    }

    public String getDim() {

        return "\033[2m";
    }

    public String getUnderline() {

        return "\033[4m";
    }

    public String getBlink() {

        return "\033[5m";
    }

    public String getInverse() {

        return "\033[7m";
    }

    public String getBlack() {

        return "\033[30m";
    }

    public String getRed() {

        return "\033[31m";
    }

    public String getGreen() {

        return "\033[32m";
    }

    public String getYellow() {

        return "\033[33m";
    }

    public String getBlue() {

        return "\033[34m";
    }

    public String getMagenta() {

        return "\033[35m";
    }

    public String getCyan() {

        return "\033[36m";
    }

    public String getWhite() {

        return "\033[37m";
    }

    public static class BackgroundTextAttribute extends TextAttribute {

        @Override
        public String getBlack() {

            return "\033[40m";
        }

        @Override
        public String getRed() {

            return "\033[41m";
        }

        @Override
        public String getGreen() {

            return "\033[42m";
        }

        @Override
        public String getYellow() {

            return "\033[43m";
        }

        @Override
        public String getBlue() {

            return "\033[44m";
        }

        @Override
        public String getMagenta() {

            return "\033[45m";
        }

        @Override
        public String getCyan() {

            return "\033[46m";
        }

        @Override
        public String getWhite() {

            return "\033[47m";
        }
    }
}
