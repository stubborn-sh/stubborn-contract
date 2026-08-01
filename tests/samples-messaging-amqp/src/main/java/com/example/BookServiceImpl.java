/*
 * Copyright 2013-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class BookServiceImpl implements BookService {

	public static final Logger LOG = LoggerFactory.getLogger(BookServiceImpl.class);

	private List<Book> books;

	private RabbitTemplate rabbitTemplate;

	@Autowired
	BookServiceImpl(RabbitTemplate rabbitTemplate) {
		this.books = new LinkedList<>();
		this.rabbitTemplate = rabbitTemplate;
	}

	@Override
	public void sendBook(Book book, String replyTo) {
		LOG.info("Received new book with bookname = " + book.getName());
		newBook(book);
		this.rabbitTemplate.convertAndSend("", replyTo, book);
	}

	@Override
	public void newBook(Book book) {
		this.books.add(book);
	}

	@Override
	public Book getBook(int index) {
		return this.books.get(index);
	}

	@Override
	public int noOfBooks() {
		return this.books.size();
	}

	@Override
	public List<Book> getBooks() {
		return this.books;
	}

}
