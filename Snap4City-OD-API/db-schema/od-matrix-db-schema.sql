--
-- PostgreSQL database dump
--

-- Dumped from database version 11.11 (Debian 11.11-0+deb10u1)
-- Dumped by pg_dump version 13.4

-- Started on 2022-03-24 12:31:59

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

--
-- TOC entry 4468 (class 1262 OID 13101)
-- Name: postgres; Type: DATABASE; Schema: -; Owner: postgres
--

CREATE DATABASE postgres WITH TEMPLATE = template0 ENCODING = 'UTF8' LOCALE = 'it_IT.UTF-8';


ALTER DATABASE postgres OWNER TO postgres;

\connect postgres

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

--
-- TOC entry 4469 (class 0 OID 0)
-- Dependencies: 4468
-- Name: DATABASE postgres; Type: COMMENT; Schema: -; Owner: postgres
--

COMMENT ON DATABASE postgres IS 'default administrative connection database';


--
-- TOC entry 2 (class 3079 OID 16386)
-- Name: postgis; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS postgis WITH SCHEMA public;


--
-- TOC entry 4470 (class 0 OID 0)
-- Dependencies: 2
-- Name: EXTENSION postgis; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION postgis IS 'PostGIS geometry, geography, and raster spatial types and functions';


SET default_tablespace = '';

--
-- TOC entry 215 (class 1259 OID 19704)
-- Name: am_com_multipart; Type: TABLE; Schema: public; Owner: debian
--

CREATE TABLE public.am_com_multipart (
    gid integer NOT NULL,
    pk_uid smallint,
    nome character varying(28),
    codcom character varying(6),
    codcatasto character varying(4),
    codprov character varying(3),
    sigla_prov character varying(2),
    distr_asl character varying(48),
    asl character varying(19),
    geom public.geometry(MultiPolygon,3003)
);


ALTER TABLE public.am_com_multipart OWNER TO debian;

--
-- TOC entry 214 (class 1259 OID 19702)
-- Name: am_com_multipart_gid_seq; Type: SEQUENCE; Schema: public; Owner: debian
--

CREATE SEQUENCE public.am_com_multipart_gid_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.am_com_multipart_gid_seq OWNER TO debian;

--
-- TOC entry 4471 (class 0 OID 0)
-- Dependencies: 214
-- Name: am_com_multipart_gid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: debian
--

ALTER SEQUENCE public.am_com_multipart_gid_seq OWNED BY public.am_com_multipart.gid;


--
-- TOC entry 219 (class 1259 OID 20894)
-- Name: cntr_rg_01m_2020_4326; Type: TABLE; Schema: public; Owner: debian
--

CREATE TABLE public.cntr_rg_01m_2020_4326 (
    gid integer NOT NULL,
    cntr_id character varying(2),
    cntr_name character varying(159),
    name_engl character varying(44),
    iso3_code character varying(3),
    fid character varying(2),
    geom public.geometry(MultiPolygon,4326)
);


ALTER TABLE public.cntr_rg_01m_2020_4326 OWNER TO debian;

--
-- TOC entry 218 (class 1259 OID 20892)
-- Name: cntr_rg_01m_2020_4326_gid_seq; Type: SEQUENCE; Schema: public; Owner: debian
--

CREATE SEQUENCE public.cntr_rg_01m_2020_4326_gid_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.cntr_rg_01m_2020_4326_gid_seq OWNER TO debian;

--
-- TOC entry 4472 (class 0 OID 0)
-- Dependencies: 218
-- Name: cntr_rg_01m_2020_4326_gid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: debian
--

ALTER SEQUENCE public.cntr_rg_01m_2020_4326_gid_seq OWNED BY public.cntr_rg_01m_2020_4326.gid;


--
-- TOC entry 221 (class 1259 OID 21146)
-- Name: comm_rg_01m_2016_4326; Type: TABLE; Schema: public; Owner: debian
--

CREATE TABLE public.comm_rg_01m_2016_4326 (
    gid integer NOT NULL,
    comm_id character varying(14),
    cntr_id character varying(2),
    cntr_code character varying(2),
    comm_name character varying(60),
    name_asci character varying(60),
    true_flag character varying(1),
    nsi_code character varying(13),
    name_nsi character varying(113),
    name_latn character varying(60),
    nuts_code character varying(5),
    fid character varying(14),
    geom public.geometry(MultiPolygon,4326)
);


ALTER TABLE public.comm_rg_01m_2016_4326 OWNER TO debian;

--
-- TOC entry 220 (class 1259 OID 21144)
-- Name: comm_rg_01m_2016_4326_gid_seq; Type: SEQUENCE; Schema: public; Owner: debian
--

CREATE SEQUENCE public.comm_rg_01m_2016_4326_gid_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.comm_rg_01m_2016_4326_gid_seq OWNER TO debian;

--
-- TOC entry 4473 (class 0 OID 0)
-- Dependencies: 220
-- Name: comm_rg_01m_2016_4326_gid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: debian
--

ALTER SEQUENCE public.comm_rg_01m_2016_4326_gid_seq OWNED BY public.comm_rg_01m_2016_4326.gid;


--
-- TOC entry 225 (class 1259 OID 22595)
-- Name: gadm36; Type: TABLE; Schema: public; Owner: debian
--

CREATE TABLE public.gadm36 (
    gid integer NOT NULL,
    uid integer,
    gid_0 character varying(80),
    id_0 integer,
    name_0 character varying(80),
    gid_1 character varying(80),
    id_1 integer,
    name_1 character varying(80),
    varname_1 character varying(129),
    nl_name_1 character varying(87),
    hasc_1 character varying(80),
    cc_1 character varying(80),
    type_1 character varying(80),
    engtype_1 character varying(80),
    validfr_1 character varying(80),
    validto_1 character varying(80),
    remarks_1 character varying(97),
    gid_2 character varying(80),
    id_2 integer,
    name_2 character varying(80),
    varname_2 character varying(116),
    nl_name_2 character varying(80),
    hasc_2 character varying(80),
    cc_2 character varying(80),
    type_2 character varying(80),
    engtype_2 character varying(80),
    validfr_2 character varying(80),
    validto_2 character varying(80),
    remarks_2 character varying(97),
    gid_3 character varying(80),
    id_3 integer,
    name_3 character varying(80),
    varname_3 character varying(80),
    nl_name_3 character varying(80),
    hasc_3 character varying(80),
    cc_3 character varying(80),
    type_3 character varying(80),
    engtype_3 character varying(80),
    validfr_3 character varying(80),
    validto_3 character varying(80),
    remarks_3 character varying(80),
    gid_4 character varying(80),
    id_4 integer,
    name_4 character varying(98),
    varname_4 character varying(80),
    cc_4 character varying(80),
    type_4 character varying(80),
    engtype_4 character varying(80),
    validfr_4 character varying(80),
    validto_4 character varying(80),
    remarks_4 character varying(80),
    gid_5 character varying(80),
    id_5 integer,
    name_5 character varying(80),
    cc_5 character varying(80),
    type_5 character varying(80),
    engtype_5 character varying(80),
    region character varying(80),
    varregion character varying(80),
    zone integer,
    geom public.geometry(MultiPolygon,4326)
);


ALTER TABLE public.gadm36 OWNER TO debian;

--
-- TOC entry 224 (class 1259 OID 22593)
-- Name: gadm36_gid_seq; Type: SEQUENCE; Schema: public; Owner: debian
--

CREATE SEQUENCE public.gadm36_gid_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.gadm36_gid_seq OWNER TO debian;

--
-- TOC entry 4474 (class 0 OID 0)
-- Dependencies: 224
-- Name: gadm36_gid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: debian
--

ALTER SEQUENCE public.gadm36_gid_seq OWNED BY public.gadm36.gid;


--
-- TOC entry 217 (class 1259 OID 20433)
-- Name: lau_rg_01m_2019_4326; Type: TABLE; Schema: public; Owner: debian
--

CREATE TABLE public.lau_rg_01m_2019_4326 (
    gid integer NOT NULL,
    gisco_id character varying(16),
    cntr_code character varying(2),
    lau_code character varying(13),
    lau_name character varying(113),
    pop_2019 integer,
    pop_dens_2 double precision,
    area_km2 double precision,
    year smallint,
    fid character varying(16),
    geom public.geometry(MultiPolygon,4326)
);


ALTER TABLE public.lau_rg_01m_2019_4326 OWNER TO debian;

--
-- TOC entry 216 (class 1259 OID 20431)
-- Name: lau_rg_01m_2019_4326_gid_seq; Type: SEQUENCE; Schema: public; Owner: debian
--

CREATE SEQUENCE public.lau_rg_01m_2019_4326_gid_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.lau_rg_01m_2019_4326_gid_seq OWNER TO debian;

--
-- TOC entry 4475 (class 0 OID 0)
-- Dependencies: 216
-- Name: lau_rg_01m_2019_4326_gid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: debian
--

ALTER SEQUENCE public.lau_rg_01m_2019_4326_gid_seq OWNED BY public.lau_rg_01m_2019_4326.gid;


--
-- TOC entry 229 (class 1259 OID 98442)
-- Name: mgrs_100kmsq_id_32t; Type: TABLE; Schema: public; Owner: debian
--

CREATE TABLE public.mgrs_100kmsq_id_32t (
    gid integer NOT NULL,
    gzd character varying(5),
    "100kmsq_id" character varying(3),
    mgrs character varying(15),
    easting character varying(10),
    northing character varying(10),
    geom public.geometry(MultiPolygon,4326)
);


ALTER TABLE public.mgrs_100kmsq_id_32t OWNER TO debian;

--
-- TOC entry 228 (class 1259 OID 98440)
-- Name: mgrs_100kmsq_id_32t_gid_seq; Type: SEQUENCE; Schema: public; Owner: debian
--

CREATE SEQUENCE public.mgrs_100kmsq_id_32t_gid_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.mgrs_100kmsq_id_32t_gid_seq OWNER TO debian;

--
-- TOC entry 4476 (class 0 OID 0)
-- Dependencies: 228
-- Name: mgrs_100kmsq_id_32t_gid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: debian
--

ALTER SEQUENCE public.mgrs_100kmsq_id_32t_gid_seq OWNED BY public.mgrs_100kmsq_id_32t.gid;


--
-- TOC entry 227 (class 1259 OID 98426)
-- Name: mgrs_1km_32t_unprojected; Type: TABLE; Schema: public; Owner: debian
--

CREATE TABLE public.mgrs_1km_32t_unprojected (
    gid integer NOT NULL,
    kmsq_id character varying(5),
    gzd character varying(4),
    easting character varying(10),
    northing character varying(10),
    shape_leng numeric,
    mgrs character varying(15),
    mgrs_10km character varying(254),
    shape_le_1 numeric,
    shape_area numeric,
    geom public.geometry(MultiPolygon,4326)
);


ALTER TABLE public.mgrs_1km_32t_unprojected OWNER TO debian;

--
-- TOC entry 226 (class 1259 OID 98424)
-- Name: mgrs_1km_32t_unprojected_gid_seq; Type: SEQUENCE; Schema: public; Owner: debian
--

CREATE SEQUENCE public.mgrs_1km_32t_unprojected_gid_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.mgrs_1km_32t_unprojected_gid_seq OWNER TO debian;

--
-- TOC entry 4477 (class 0 OID 0)
-- Dependencies: 226
-- Name: mgrs_1km_32t_unprojected_gid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: debian
--

ALTER SEQUENCE public.mgrs_1km_32t_unprojected_gid_seq OWNED BY public.mgrs_1km_32t_unprojected.gid;


--
-- TOC entry 222 (class 1259 OID 21797)
-- Name: od_data; Type: TABLE; Schema: public; Owner: debian
--

CREATE TABLE public.od_data (
    od_id character varying,
    x_orig numeric,
    y_orig numeric,
    x_dest numeric,
    y_dest numeric,
    "precision" bigint,
    value numeric,
    orig_geom public.geometry(Polygon,4326),
    dest_geom public.geometry(Polygon,4326),
    from_date timestamp(0) without time zone,
    to_date timestamp(0) without time zone,
    orig_commune integer,
    dest_commune integer
);


ALTER TABLE public.od_data OWNER TO debian;

--
-- TOC entry 230 (class 1259 OID 110262)
-- Name: od_data_mgrs; Type: TABLE; Schema: public; Owner: debian
--

CREATE TABLE public.od_data_mgrs (
    od_id character varying,
    x_orig numeric,
    y_orig numeric,
    x_dest numeric,
    y_dest numeric,
    "precision" bigint,
    value numeric,
    orig_geom public.geometry(Polygon,4326),
    dest_geom public.geometry(Polygon,4326),
    from_date timestamp(0) without time zone,
    to_date timestamp(0) without time zone,
    orig_commune character varying,
    dest_commune character varying
);


ALTER TABLE public.od_data_mgrs OWNER TO debian;

--
-- TOC entry 212 (class 1259 OID 19346)
-- Name: od_data_mgrs_old; Type: TABLE; Schema: public; Owner: debian
--

CREATE TABLE public.od_data_mgrs_old (
    od_id character varying,
    x_orig numeric,
    y_orig numeric,
    x_dest numeric,
    y_dest numeric,
    "precision" bigint,
    value numeric,
    orig_geom public.geometry(Polygon,4326),
    dest_geom public.geometry(Polygon,4326),
    from_date timestamp(0) without time zone,
    to_date timestamp(0) without time zone,
    orig_commune character varying,
    dest_commune character varying
);


ALTER TABLE public.od_data_mgrs_old OWNER TO debian;

--
-- TOC entry 223 (class 1259 OID 21805)
-- Name: od_metadata; Type: TABLE; Schema: public; Owner: debian
--

CREATE TABLE public.od_metadata (
    od_id character varying,
    value_type character varying,
    value_unit character varying,
    description character varying,
    organization character varying,
    kind character varying,
    mode character varying,
    transport character varying,
    purpose character varying
);


ALTER TABLE public.od_metadata OWNER TO debian;

--
-- TOC entry 213 (class 1259 OID 19356)
-- Name: od_metadata_mgrs; Type: TABLE; Schema: public; Owner: debian
--

CREATE TABLE public.od_metadata_mgrs (
    od_id character varying,
    value_type character varying,
    value_unit character varying,
    description character varying,
    organization character varying,
    kind character varying,
    mode character varying,
    transport character varying,
    purpose character varying
);


ALTER TABLE public.od_metadata_mgrs OWNER TO debian;

--
-- TOC entry 4297 (class 2604 OID 19707)
-- Name: am_com_multipart gid; Type: DEFAULT; Schema: public; Owner: debian
--

ALTER TABLE ONLY public.am_com_multipart ALTER COLUMN gid SET DEFAULT nextval('public.am_com_multipart_gid_seq'::regclass);


--
-- TOC entry 4299 (class 2604 OID 20897)
-- Name: cntr_rg_01m_2020_4326 gid; Type: DEFAULT; Schema: public; Owner: debian
--

ALTER TABLE ONLY public.cntr_rg_01m_2020_4326 ALTER COLUMN gid SET DEFAULT nextval('public.cntr_rg_01m_2020_4326_gid_seq'::regclass);


--
-- TOC entry 4300 (class 2604 OID 21149)
-- Name: comm_rg_01m_2016_4326 gid; Type: DEFAULT; Schema: public; Owner: debian
--

ALTER TABLE ONLY public.comm_rg_01m_2016_4326 ALTER COLUMN gid SET DEFAULT nextval('public.comm_rg_01m_2016_4326_gid_seq'::regclass);


--
-- TOC entry 4301 (class 2604 OID 22598)
-- Name: gadm36 gid; Type: DEFAULT; Schema: public; Owner: debian
--

ALTER TABLE ONLY public.gadm36 ALTER COLUMN gid SET DEFAULT nextval('public.gadm36_gid_seq'::regclass);


--
-- TOC entry 4298 (class 2604 OID 20436)
-- Name: lau_rg_01m_2019_4326 gid; Type: DEFAULT; Schema: public; Owner: debian
--

ALTER TABLE ONLY public.lau_rg_01m_2019_4326 ALTER COLUMN gid SET DEFAULT nextval('public.lau_rg_01m_2019_4326_gid_seq'::regclass);


--
-- TOC entry 4303 (class 2604 OID 98445)
-- Name: mgrs_100kmsq_id_32t gid; Type: DEFAULT; Schema: public; Owner: debian
--

ALTER TABLE ONLY public.mgrs_100kmsq_id_32t ALTER COLUMN gid SET DEFAULT nextval('public.mgrs_100kmsq_id_32t_gid_seq'::regclass);


--
-- TOC entry 4302 (class 2604 OID 98429)
-- Name: mgrs_1km_32t_unprojected gid; Type: DEFAULT; Schema: public; Owner: debian
--

ALTER TABLE ONLY public.mgrs_1km_32t_unprojected ALTER COLUMN gid SET DEFAULT nextval('public.mgrs_1km_32t_unprojected_gid_seq'::regclass);


--
-- TOC entry 4311 (class 2606 OID 19709)
-- Name: am_com_multipart am_com_multipart_pkey; Type: CONSTRAINT; Schema: public; Owner: debian
--

ALTER TABLE ONLY public.am_com_multipart
    ADD CONSTRAINT am_com_multipart_pkey PRIMARY KEY (gid);


--
-- TOC entry 4317 (class 2606 OID 20899)
-- Name: cntr_rg_01m_2020_4326 cntr_rg_01m_2020_4326_pkey; Type: CONSTRAINT; Schema: public; Owner: debian
--

ALTER TABLE ONLY public.cntr_rg_01m_2020_4326
    ADD CONSTRAINT cntr_rg_01m_2020_4326_pkey PRIMARY KEY (gid);


--
-- TOC entry 4320 (class 2606 OID 21151)
-- Name: comm_rg_01m_2016_4326 comm_rg_01m_2016_4326_pkey; Type: CONSTRAINT; Schema: public; Owner: debian
--

ALTER TABLE ONLY public.comm_rg_01m_2016_4326
    ADD CONSTRAINT comm_rg_01m_2016_4326_pkey PRIMARY KEY (gid);


--
-- TOC entry 4326 (class 2606 OID 22603)
-- Name: gadm36 gadm36_pkey; Type: CONSTRAINT; Schema: public; Owner: debian
--

ALTER TABLE ONLY public.gadm36
    ADD CONSTRAINT gadm36_pkey PRIMARY KEY (gid);


--
-- TOC entry 4314 (class 2606 OID 20438)
-- Name: lau_rg_01m_2019_4326 lau_rg_01m_2019_4326_pkey; Type: CONSTRAINT; Schema: public; Owner: debian
--

ALTER TABLE ONLY public.lau_rg_01m_2019_4326
    ADD CONSTRAINT lau_rg_01m_2019_4326_pkey PRIMARY KEY (gid);


--
-- TOC entry 4332 (class 2606 OID 98447)
-- Name: mgrs_100kmsq_id_32t mgrs_100kmsq_id_32t_pkey; Type: CONSTRAINT; Schema: public; Owner: debian
--

ALTER TABLE ONLY public.mgrs_100kmsq_id_32t
    ADD CONSTRAINT mgrs_100kmsq_id_32t_pkey PRIMARY KEY (gid);


--
-- TOC entry 4329 (class 2606 OID 98434)
-- Name: mgrs_1km_32t_unprojected mgrs_1km_32t_unprojected_pkey; Type: CONSTRAINT; Schema: public; Owner: debian
--

ALTER TABLE ONLY public.mgrs_1km_32t_unprojected
    ADD CONSTRAINT mgrs_1km_32t_unprojected_pkey PRIMARY KEY (gid);


--
-- TOC entry 4309 (class 1259 OID 20093)
-- Name: am_com_multipart_geom_idx; Type: INDEX; Schema: public; Owner: debian
--

CREATE INDEX am_com_multipart_geom_idx ON public.am_com_multipart USING gist (geom);


--
-- TOC entry 4315 (class 1259 OID 21142)
-- Name: cntr_rg_01m_2020_4326_geom_idx; Type: INDEX; Schema: public; Owner: debian
--

CREATE INDEX cntr_rg_01m_2020_4326_geom_idx ON public.cntr_rg_01m_2020_4326 USING gist (geom);


--
-- TOC entry 4318 (class 1259 OID 21786)
-- Name: comm_rg_01m_2016_4326_geom_idx; Type: INDEX; Schema: public; Owner: debian
--

CREATE INDEX comm_rg_01m_2016_4326_geom_idx ON public.comm_rg_01m_2016_4326 USING gist (geom);


--
-- TOC entry 4321 (class 1259 OID 21803)
-- Name: dest_geom; Type: INDEX; Schema: public; Owner: debian
--

CREATE INDEX dest_geom ON public.od_data USING gist (dest_geom);


--
-- TOC entry 4306 (class 1259 OID 19354)
-- Name: dest_geom_1; Type: INDEX; Schema: public; Owner: debian
--

CREATE INDEX dest_geom_1 ON public.od_data_mgrs_old USING gist (dest_geom);


--
-- TOC entry 4333 (class 1259 OID 110269)
-- Name: dest_geom_index; Type: INDEX; Schema: public; Owner: debian
--

CREATE INDEX dest_geom_index ON public.od_data_mgrs USING gist (dest_geom);


--
-- TOC entry 4324 (class 1259 OID 98060)
-- Name: gadm36_geom_idx; Type: INDEX; Schema: public; Owner: debian
--

CREATE INDEX gadm36_geom_idx ON public.gadm36 USING gist (geom);


--
-- TOC entry 4312 (class 1259 OID 20645)
-- Name: lau_rg_01m_2019_4326_geom_idx; Type: INDEX; Schema: public; Owner: debian
--

CREATE INDEX lau_rg_01m_2019_4326_geom_idx ON public.lau_rg_01m_2019_4326 USING gist (geom);


--
-- TOC entry 4330 (class 1259 OID 98451)
-- Name: mgrs_100kmsq_id_32t_geom_idx; Type: INDEX; Schema: public; Owner: debian
--

CREATE INDEX mgrs_100kmsq_id_32t_geom_idx ON public.mgrs_100kmsq_id_32t USING gist (geom);


--
-- TOC entry 4327 (class 1259 OID 98435)
-- Name: mgrs_1km_32t_unprojected_geom_idx; Type: INDEX; Schema: public; Owner: debian
--

CREATE INDEX mgrs_1km_32t_unprojected_geom_idx ON public.mgrs_1km_32t_unprojected USING gist (geom);


--
-- TOC entry 4323 (class 1259 OID 21811)
-- Name: od_metadata_od_id_idx; Type: INDEX; Schema: public; Owner: debian
--

CREATE UNIQUE INDEX od_metadata_od_id_idx ON public.od_metadata USING btree (od_id);


--
-- TOC entry 4308 (class 1259 OID 19362)
-- Name: od_metadata_od_id_idx_1; Type: INDEX; Schema: public; Owner: debian
--

CREATE UNIQUE INDEX od_metadata_od_id_idx_1 ON public.od_metadata_mgrs USING btree (od_id);


--
-- TOC entry 4322 (class 1259 OID 21804)
-- Name: orig_geom; Type: INDEX; Schema: public; Owner: debian
--

CREATE INDEX orig_geom ON public.od_data USING gist (orig_geom);


--
-- TOC entry 4307 (class 1259 OID 19355)
-- Name: orig_geom_1; Type: INDEX; Schema: public; Owner: debian
--

CREATE INDEX orig_geom_1 ON public.od_data_mgrs_old USING gist (orig_geom);


--
-- TOC entry 4334 (class 1259 OID 110270)
-- Name: orig_geom_index; Type: INDEX; Schema: public; Owner: debian
--

CREATE INDEX orig_geom_index ON public.od_data_mgrs USING gist (orig_geom);


-- Completed on 2022-03-24 12:32:07

--
-- PostgreSQL database dump complete
--

