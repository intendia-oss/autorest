package com.intendia.gwt.autorest.client;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;

public class JreResourceBuilder extends CollectorResourceVisitor {
    private final ConnectionFactory factory;
    private final JsonCodec json;

    public JreResourceBuilder(String root) {
        this(root, url -> (HttpURLConnection) new URL(url).openConnection(), new GsonCodec());
    }

    public JreResourceBuilder(String root, ConnectionFactory factory, JsonCodec codec) {
        this.factory = factory;
        this.json = codec;
        path(root);
    }

    @Override protected String encodeComponent(String str) {
        try {
            return URLEncoder.encode(str, "UTF-8")
                    .replaceAll("%21", "!").replaceAll("%27", "'")
                    .replaceAll("%28", "(").replaceAll("%29", ")")
                    .replaceAll("%7E", "~");
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Override public <T> T as(TypeToken<T> typeToken) {
        return json.fromJson(request(), typeToken);
    }

    private Single<Reader> request() {
        return Single.using(() -> {
            HttpURLConnection req;
            try {
                req = factory.apply(uri());
                req.setRequestMethod(method);
                if (produces.length > 0) req.setRequestProperty("Accept", produces[0]);
                for (Param<?> e : headerParams) req.setRequestProperty(e.k, Objects.toString(e.v));
            } catch (Exception e) {
                throw err("open connection error", e);
            }
            if (data != null) {
                req.setRequestProperty("Content-Type", Stream.of(consumes).findFirst()
                        .orElse("application/json"));
                req.setDoOutput(true);
                try (OutputStreamWriter out = new OutputStreamWriter(req.getOutputStream())) {
                    json.toJson(data, out);
                } catch (Exception e) {
                    throw err("writing stream error", e);
                }
            } else if (!formParams.isEmpty()) {
                req.setRequestProperty("Content-Type", Stream.of(consumes).findFirst()
                        .orElse("application/x-www-form-urlencoded"));
                req.setDoOutput(true);
                try (OutputStreamWriter out = new OutputStreamWriter(req.getOutputStream())) {
                    String csq = encodeParams(formParams);
                    out.append(csq);
                } catch (Exception e) {
                    throw err("writing stream error", e);
                }
            }
            Reader reader;
            try {
                reader = new InputStreamReader(req.getInputStream());
                int rc = req.getResponseCode();
                if (rc != 200 && rc != 201 && rc != 204) {
                    throw new RuntimeException("unexpected response code " + rc);
                }
            } catch (IOException e) {
                throw err("reading stream error", e);
            }
            return reader;
        }, Single::just, reader -> {
            try { reader.close(); } catch (IOException e) { throw err("closing response error", e); }
        }, false/*late dispose*/);
    }

    private static RuntimeException err(String msg, Exception e) { return new RuntimeException(msg + ": " + e, e); }

    @FunctionalInterface
    public interface ConnectionFactory {
        HttpURLConnection apply(String uri) throws Exception;
    }

    public interface JsonCodec {
        void toJson(Object src, Appendable writer);
        <C> C fromJson(Single<Reader> json, TypeToken<C> typeToken);
    }

    public static class GsonCodec implements JsonCodec {
        private final Gson gson = new Gson();

        @Override public void toJson(Object src, Appendable writer) {
            gson.toJson(src, writer);
        }

        @SuppressWarnings("unchecked")
        @Override public <T> T fromJson(Single<Reader> req, TypeToken<T> typeToken) {
            if (Completable.class.equals(typeToken.getRawType())) return (T) req.doOnSuccess(this::consume).toCompletable();
            if (Single.class.equals(typeToken.getRawType())) return (T) req.map(reader -> {
            	TypeToken<?> itemTypeToken = typeToken.getTypeArguments()[0];
            	
                if (Reader.class.equals(itemTypeToken.getRawType())) return reader;
                if (String.class.equals(itemTypeToken.getRawType())) return readAsString(reader);
                return gson.fromJson(reader, itemTypeToken.getType());
            });
            if (Observable.class.equals(typeToken.getRawType())) {
            	TypeToken<?> itemTypeToken = typeToken.getTypeArguments()[0];
            	
            	return (T) req.toObservable()
                    .flatMapIterable(n -> () -> new ParseArrayIterator<>(n, itemTypeToken));
            }
        	
            throw new IllegalArgumentException("unsupported type " + typeToken);
        }

        private static String readAsString(Reader in) {
            try {
                StringBuilder out = new StringBuilder();
                int ch; while ((ch = in.read()) != -1) out.append((char) ch);
                in.close();
                return out.toString();
            } catch (IOException e) {
                throw new RuntimeException("error reading request data", e);
            }
        }

        private class ParseArrayIterator<T> implements Iterator<T> {
            private final TypeToken<T> typeToken;
            private JsonReader reader;
            public ParseArrayIterator(Reader reader, TypeToken<T> typeToken) {
                this.typeToken = typeToken;
                this.reader = new JsonReader(reader);
                try { this.reader.beginArray(); } catch (Exception e) { throw err("parsing error", e); }
            }
            @Override public boolean hasNext() {
                try {
                    return reader != null && reader.hasNext();
                } catch (Exception e) { throw err("parsing error", e); }
            }
            @Override public T next() {
                if (!hasNext()) throw new NoSuchElementException();
                try {
                    T next = gson.fromJson(reader, typeToken.getType());
                    if (!reader.hasNext()) { reader.endArray(); reader.close(); reader = null; }
                    return next;
                } catch (Exception e) { throw err("parsing error", e); }
            }
        }

        /** Consume network buffer, some SO might have problems if not. */
        private void consume(Reader n) { try { while (n.read() != -1) ; } catch (IOException ignore) {/**/} }
    }
}
