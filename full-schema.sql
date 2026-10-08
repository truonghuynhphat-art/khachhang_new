--
-- PostgreSQL database dump
--

\restrict yeMQgYa8ukR55rfEONDcASmq9hF8HLJ4E4rx3Wa7Qab0EUNPVweG9YNE6jrw92X

-- Dumped from database version 16.15 (Ubuntu 16.15-0ubuntu0.24.04.1)
-- Dumped by pg_dump version 16.15 (Ubuntu 16.15-0ubuntu0.24.04.1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: account; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.account (
    id bigint NOT NULL,
    account_number character varying(50) NOT NULL,
    balance numeric(19,2) NOT NULL,
    customer_id bigint NOT NULL,
    transaction_limit numeric(19,2) NOT NULL,
    opened_date date NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL
);


--
-- Name: account_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.account_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: account_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.account_id_seq OWNED BY public.account.id;


--
-- Name: account_status_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.account_status_history (
    id bigint NOT NULL,
    account_id bigint NOT NULL,
    old_status character varying(20) NOT NULL,
    new_status character varying(20) NOT NULL,
    reason character varying(255),
    changed_at timestamp without time zone NOT NULL
);


--
-- Name: account_status_history_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.account_status_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: account_status_history_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.account_status_history_id_seq OWNED BY public.account_status_history.id;


--
-- Name: alert; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.alert (
    id bigint NOT NULL,
    transaction_id bigint NOT NULL,
    type character varying(30) NOT NULL,
    description character varying(255) NOT NULL,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    created_at timestamp without time zone NOT NULL
);


--
-- Name: alert_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.alert_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: alert_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.alert_id_seq OWNED BY public.alert.id;


--
-- Name: customer; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customer (
    id bigint NOT NULL,
    name character varying(100) NOT NULL,
    location character varying(100),
    customer_type_id bigint NOT NULL
);


--
-- Name: customer_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.customer_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: customer_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.customer_id_seq OWNED BY public.customer.id;


--
-- Name: customer_type; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customer_type (
    id bigint NOT NULL,
    name character varying(20) NOT NULL,
    max_transaction_limit numeric(19,2) NOT NULL
);


--
-- Name: customer_type_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.customer_type_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: customer_type_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.customer_type_id_seq OWNED BY public.customer_type.id;


--
-- Name: recurring_transaction; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.recurring_transaction (
    id bigint NOT NULL,
    account_id bigint NOT NULL,
    type character varying(20) NOT NULL,
    amount numeric(19,2) NOT NULL,
    location character varying(100),
    frequency character varying(20) NOT NULL,
    next_run_date timestamp without time zone NOT NULL,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone NOT NULL
);


--
-- Name: recurring_transaction_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.recurring_transaction_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: recurring_transaction_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.recurring_transaction_id_seq OWNED BY public.recurring_transaction.id;


--
-- Name: transactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.transactions (
    id bigint NOT NULL,
    account_id bigint NOT NULL,
    type character varying(30) NOT NULL,
    amount numeric(19,2) NOT NULL,
    fee numeric(19,2) NOT NULL,
    location character varying(100),
    created_at timestamp without time zone NOT NULL,
    active boolean DEFAULT true NOT NULL
);


--
-- Name: transactions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.transactions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: transactions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.transactions_id_seq OWNED BY public.transactions.id;


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id bigint NOT NULL,
    username character varying(50) NOT NULL,
    password character varying(100) NOT NULL,
    role character varying(20) NOT NULL,
    customer_id bigint
);


--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;


--
-- Name: account id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.account ALTER COLUMN id SET DEFAULT nextval('public.account_id_seq'::regclass);


--
-- Name: account_status_history id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.account_status_history ALTER COLUMN id SET DEFAULT nextval('public.account_status_history_id_seq'::regclass);


--
-- Name: alert id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.alert ALTER COLUMN id SET DEFAULT nextval('public.alert_id_seq'::regclass);


--
-- Name: customer id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer ALTER COLUMN id SET DEFAULT nextval('public.customer_id_seq'::regclass);


--
-- Name: customer_type id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_type ALTER COLUMN id SET DEFAULT nextval('public.customer_type_id_seq'::regclass);


--
-- Name: recurring_transaction id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recurring_transaction ALTER COLUMN id SET DEFAULT nextval('public.recurring_transaction_id_seq'::regclass);


--
-- Name: transactions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transactions ALTER COLUMN id SET DEFAULT nextval('public.transactions_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- Name: account account_account_number_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.account
    ADD CONSTRAINT account_account_number_key UNIQUE (account_number);


--
-- Name: account account_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.account
    ADD CONSTRAINT account_pkey PRIMARY KEY (id);


--
-- Name: account_status_history account_status_history_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.account_status_history
    ADD CONSTRAINT account_status_history_pkey PRIMARY KEY (id);


--
-- Name: alert alert_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.alert
    ADD CONSTRAINT alert_pkey PRIMARY KEY (id);


--
-- Name: customer customer_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer
    ADD CONSTRAINT customer_pkey PRIMARY KEY (id);


--
-- Name: customer_type customer_type_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_type
    ADD CONSTRAINT customer_type_name_key UNIQUE (name);


--
-- Name: customer_type customer_type_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_type
    ADD CONSTRAINT customer_type_pkey PRIMARY KEY (id);


--
-- Name: recurring_transaction recurring_transaction_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recurring_transaction
    ADD CONSTRAINT recurring_transaction_pkey PRIMARY KEY (id);


--
-- Name: transactions transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transactions
    ADD CONSTRAINT transactions_pkey PRIMARY KEY (id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: users users_username_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_username_key UNIQUE (username);


--
-- Name: account_status_history account_status_history_account_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.account_status_history
    ADD CONSTRAINT account_status_history_account_id_fkey FOREIGN KEY (account_id) REFERENCES public.account(id);


--
-- Name: alert alert_transaction_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.alert
    ADD CONSTRAINT alert_transaction_id_fkey FOREIGN KEY (transaction_id) REFERENCES public.transactions(id);


--
-- Name: customer customer_customer_type_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer
    ADD CONSTRAINT customer_customer_type_id_fkey FOREIGN KEY (customer_type_id) REFERENCES public.customer_type(id);


--
-- Name: account fk_account_customer; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.account
    ADD CONSTRAINT fk_account_customer FOREIGN KEY (customer_id) REFERENCES public.customer(id);


--
-- Name: transactions fk_transaction_account; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transactions
    ADD CONSTRAINT fk_transaction_account FOREIGN KEY (account_id) REFERENCES public.account(id);


--
-- Name: recurring_transaction recurring_transaction_account_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recurring_transaction
    ADD CONSTRAINT recurring_transaction_account_id_fkey FOREIGN KEY (account_id) REFERENCES public.account(id);


--
-- Name: users users_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.customer(id);


--
-- PostgreSQL database dump complete
--

\unrestrict yeMQgYa8ukR55rfEONDcASmq9hF8HLJ4E4rx3Wa7Qab0EUNPVweG9YNE6jrw92X

