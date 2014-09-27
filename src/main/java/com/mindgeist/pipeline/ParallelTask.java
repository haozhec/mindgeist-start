package com.mindgeist.pipeline;

import java.util.List;
import java.util.function.Function;

import rx.Observable;

public class ParallelTask<T,R> extends Task<T>{
	protected Observable<R> chain = null;
	
	public ParallelTask(List<T> list) {
		super();
		observable = Observable.from(list);
	}

	public ParallelTask<T,R> parallel(Function<T,Task<R>> func) {
		chain = observable.parallel(ot -> ot.flatMap(t -> func.apply(t).observable));
		return this;
	}
	
	public Task<List<R>> toTask() {
		Task<List<R>> task = new Task<List<R>>();
		task.observable = chain.toList();
		return task;
	}

}
