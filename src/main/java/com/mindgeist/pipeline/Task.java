package com.mindgeist.pipeline;

import static rx.Observable.just;

import java.util.function.Function;

import rx.Observable;
import rx.schedulers.Schedulers;

public class Task<T> {
	protected Observable<T> observable = null;
	
	public Task(T t) {
		observable = just(t).subscribeOn(Schedulers.io());
	}
	
	protected Task() {}

	public <R> Task<R> next(Function<T,Observable<R>> func){
		Task<R> task = new Task<R>();
		task.observable = observable.flatMap(i -> func.apply(i));
		return task;
	}
	
	public T eval() {
		return this.observable.toBlocking().single();
	}
}
