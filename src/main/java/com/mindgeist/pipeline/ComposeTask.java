package com.mindgeist.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import rx.Observable;
import rx.functions.FuncN;

public class ComposeTask<R> {
	private List<Observable<?>> tasks = new ArrayList<Observable<?>>();
	
	public <T> ComposeTask<R> append(Task<T> task) {
		tasks.add(task.observable);
		return this;
	}
	
	@SuppressWarnings("unchecked")
	public <T1,T2> Task<R> then(BiFunction<T1,T2,R> func) {
		Task<R> task = new Task<R>();
		task.observable = Observable.zip(
				tasks.get(0), 
				tasks.get(1), 
				(t1,t2) -> func.apply((T1)t1,(T2)t2)
		);
				
		return task;
	}

	public Task<R> then(FuncN<R> func) {
		Task<R> task = new Task<R>();
		task.observable = Observable.zip(tasks, func);
				
		return task;
	}

}
