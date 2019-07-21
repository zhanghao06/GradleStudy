/*
 * Copyright (C) 2019 Baidu, Inc. All Rights Reserved.
 */
package com.example.book;

import com.example.componentlib.IlikeApplication;

public class BookLikeApplication implements IlikeApplication {
    @Override
    public void onCreate() {
        System.out.println("BookLikeApplication onCreate");
    }
}
