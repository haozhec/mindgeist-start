package com.mindgeist.pipeline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static rx.Observable.just;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.functions.Func1;
import rx.functions.FuncN;
import rx.schedulers.Schedulers;

public class ObservableTest {
	private final Logger logger = LoggerFactory.getLogger(ObservableTest.class);
	
	private Server server = new Server();
	
	public class Server {
		public Observable<Integer> increase(int i) {
			logger.info("-> [increase] {}", i);
			return Observable.defer(() -> {
				try {
					Thread.sleep(100);
					logger.info("<- [increase] {}", i+1);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return just(i+1); 
		    });
		}
		
		public Observable<String> toStr(int i) {
			logger.info("-> [toString] {}", i);
			return Observable.defer(() -> {
				try {
					Thread.sleep(100);
					logger.info("<- [toString] {}", String.valueOf(i));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return just(String.valueOf(i)); 
		    });
		}
	}
	
	@Test
	public void testSequenceWithObservable() {
		String twoStr = just(1).flatMap(server::increase).flatMap(server::toStr).toBlocking().single();
		assertEquals("2", twoStr);
	}
	
	@Test
	public void testSequence() {
		Task<Integer> start = new Task<Integer>(1);
		
		String twoStr = start.next(server::increase).next(server::toStr).eval();
		assertEquals("2", twoStr);
	}
	
	@Test
	public void testParallelWithObservable() {
		Func1<Observable<Integer>, Observable<String>> chain = (oi) -> { 
			return oi.flatMap(server::increase)
			.flatMap(server::toStr);
		};
			
		List<String> results = Observable.from(Arrays.asList(1,2,3)).parallel(chain).toList().toBlocking().single();
		
		assertEquals(3, results.size());
		assertTrue(results.containsAll(Arrays.asList("2", "3", "4")));
	}
	
	@Test
	public void testParallel() {
		List<String> results = new ParallelTask<Integer,String>(Arrays.asList(1,2,3)).parallel(i -> {
			return new Task<Integer>(i).next(server::increase).next(server::toStr);
		}).toTask().eval();
		
		assertEquals(3, results.size());
		assertTrue(results.containsAll(Arrays.asList("2", "3", "4")));
	}
	
	@Test
	public void testComposeWithObservable() {
		String result = Observable.zip(
				just(1).subscribeOn(Schedulers.io()).flatMap(server::increase),
				just(1).subscribeOn(Schedulers.io()).flatMap(server::toStr),
				(i,s) -> "string: "+s+", int: "+i
		).toBlocking().single();
		
		assertEquals("string: 1, int: 2", result);
	}
	
	@Test
	public void testCompose() {
		String result = new ComposeTask<String>().append(
			new Task<Integer>(1).next(server::increase)
		).append(
			new Task<Integer>(1).next(server::toStr)
		).then((i, s) -> "string: "+s+", int: "+i)
		.eval();
		
		assertEquals("string: 1, int: 2", result);
	}
	
	@Test
	public void testCompleteWithObservable() {
		String result = Observable.zip(
				Observable.from(Arrays.asList(1,2,3)).parallel(
						(oi) -> oi.flatMap(server::increase).flatMap(server::toStr)
				).toList(),
				just(1).subscribeOn(Schedulers.io()).flatMap(server::increase).flatMap(server::toStr),
				(l,s) -> {
					Collections.sort(l);
					return "list: " + l + ", string: "+s;
				}
		).toBlocking().single();
	
		assertEquals("list: [2, 3, 4], string: 2", result);
	}
	
	@Test
	public void testComplete() {
		@SuppressWarnings("unchecked")
		FuncN<String> aggregate = (args) -> {
			List<String> l = (List<String>) args[0];
			Collections.sort(l);
			String s = (String) args[1];
			return "list: " + l + ", string: "+s.toString();
		};
		String result = new ComposeTask<String>().append(
				new ParallelTask<Integer,String>(Arrays.asList(1,2,3)).parallel(
						i -> new Task<Integer>(i).next(server::increase).next(server::toStr)
				).toTask()
			).append( new Task<Integer>(1).next(server::increase).next(server::toStr))
		.then(aggregate).eval();
	
		assertEquals("list: [2, 3, 4], string: 2", result);
	}
}
