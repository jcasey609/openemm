/*	-*- mode: c; mode: fold -*-	*/
/*********************************************************************************
 * The contents of this file are subject to the OpenEMM Public License Version 1.1
 * ("License"); You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.agnitas.org/openemm.
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied.  See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Original Code is OpenEMM.
 * The Initial Developer of the Original Code is AGNITAS AG. Portions created by
 * AGNITAS AG are Copyright (C) 2006 AGNITAS AG. All Rights Reserved.
 *
 * All copies of the Covered Code must include on each user interface screen,
 * visible to all users at all times
 *    (a) the OpenEMM logo in the upper left corner and
 *    (b) the OpenEMM copyright notice at the very bottom center
 * See full license, exhibit B for requirements.
 ********************************************************************************/
# include	<stdio.h>
# include	<stdlib.h>
# include	<unistd.h>
# include	<string.h>
# include	<signal.h>
# include	<errno.h>
# include	<sys/types.h>
# include	<sys/select.h>
# include	<sa.h>
# include	"bavwrap.h"

static int
read_mail (store_t *st, log_t *lg, int timeout, int head_only) /*{{{*/
{
	int		rc;
	int		state;
	int		fd;
	fd_set		fset;
	struct timeval	tv;
	int		n, m;
	byte_t		buf[8192];
	
	rc = -1;
	state = 0;
	fd = STDIN_FILENO;
	FD_ZERO (& fset);
	while (rc == -1) {
		FD_SET (fd, & fset);
		tv.tv_usec = 0;
		tv.tv_sec = timeout;
		if (((n = select (fd + 1, & fset, NULL, NULL, & tv)) == 1) && FD_ISSET (fd, & fset)) {
			if ((n = read (fd, buf, sizeof (buf))) > 0) {
				if (head_only) {
					for (m = 0; (m < n) && (state < 2); ++m)
						if (buf[m] == '\n')
							++state;
						else if (buf[m] != '\r')
							state = 0;
					if ((m > 0) && (store_add (st, buf, m) == -1)) {
						log_out (lg, LV_ERROR, "Failed to store %d head bytes", m);
						break;
					}
					if (state == 2)
						rc = 0;
				} else
					if (store_add (st, buf, n) == -1) {
						log_out (lg, LV_ERROR, "Failed to store %d bytes", n);
						break;
					}
			} else if (n == 0)
				rc = 0;
		} else if ((n != -1) || (errno != EINTR)) {
			log_out (lg, LV_ERROR, "Ran into timeout while waiting for input (select %d, errno %d)", n, errno);
			break;
		}
	}
	return rc;
}/*}}}*/
static char *
nextline (byte_t **buf) /*{{{*/
{
	byte_t	*ptr = *buf;
	char	*line;
	
	if (*ptr == 0)
		return NULL;
	line = (char *) ptr;
	while (*ptr && (*ptr != '\r') && (*ptr != '\n'))
		++ptr;
	if (*ptr == '\r')
		*ptr++ = 0;
	if (*ptr == '\n')
		++ptr;
	*buf = ptr;
	return line;
}/*}}}*/
static void
parse_exit_code (byte_t *buf, int *exit_code) /*{{{*/
{
	byte_t	*ptr;
	char	*line, *temp;

	puts (buf);
	ptr = buf;
	*exit_code = 8;
	if (line = nextline (& ptr)) {
		temp = line;
		line = skip (line);
		if (! strncmp (temp, "HTTP/", 5)) {
			temp = line;
			line = skip (line);
			if ((! strcmp (temp, "200")) && (! strcmp (line, "OK"))) {
				while (line = nextline (& ptr))
					if (! *line)
						break;
				if (line && (line = nextline (& ptr)))
					if (! strncmp (line, "+OK", 3))
						*exit_code = 0;
					else if (! strncmp (line, "-ERR", 4))
						*exit_code = 9;
			}
		}
	}
}/*}}}*/
static int
to_bav (store_t *st, log_t *lg, int timeout, const char *call, int *exit_code) /*{{{*/
{
	int		rc;
	sa_rc_t		code;
	sa_addr_t	*addr;

	rc = -1;
	if ((code = sa_addr_create (& addr)) != SA_OK) {
		log_out (lg, LV_ERROR, "Unable to create address object %d", code);
		return -1;
	}
	if ((code = sa_addr_u2a (addr, "inet://127.0.0.1:5166")) != SA_OK)
		log_out (lg, LV_ERROR, "Unable to setup address object %d", code);
	else {
		sa_t	*sd;
		
		if ((code = sa_create (& sd)) != SA_OK)
			log_out (lg, LV_ERROR, "Unable to setup socket object %d", code);
		else if ((code = sa_type (sd, SA_TYPE_STREAM)) != SA_OK)
			log_out (lg, LV_ERROR, "Unable to set socket object to stream %d", code);
		else if ((code = sa_timeout (sd, SA_TIMEOUT_ALL, timeout, 0)) != SA_OK)
			log_out (lg, LV_ERROR, "Unable to setup socket object timeouts %d", code);
		else if ((code = sa_connect (sd, addr)) != SA_OK)
			log_out (lg, LV_ERROR, "Failed to connect to bavd %d", code);
		else {
			if ((code = sa_writef (sd, "POST /%s HTTP/1.1\r\nContent-Length: %d\r\n\r\n", call, st -> size)) != SA_OK)
				log_out (lg, LV_ERROR, "Failed to send header to bavd %d", code);
			else {
				byte_t	buf[8192];
				byte_t	*ptr;
				int	got, len;
				size_t	written, received;
				int	failcount;

				rc = 0;
				store_rewind (st);
				got = 0;
				failcount = 0;
				while (got < st -> size) {
					len = store_get (st, buf, sizeof (buf));
					ptr = buf;
					got += len;
					while (len > 0) {
						if ((code = sa_write (sd, ptr, len, & written)) != SA_OK) {
							++failcount;
							log_out (lg, LV_ERROR, "Failed to write data %d (%d)", code, failcount);
							if ((code == SA_ERR_TMT) || (code == SA_ERR_SYS) || (code == SA_ERR_NET)) {
								if (failcount > 8)
									rc = -1;
							} else
								rc = -1;
							if (rc == -1)
								break;
						} else
							failcount = 0;
						len -= written;
						ptr += written;
					}
				}
				if ((rc == 0) && (got < st -> size))
					rc = -1;
				if (rc == 0) {
					rc = -1;
					if ((code = sa_flush (sd)) != SA_OK)
						log_out (lg, LV_ERROR, "Failed to flush data %d", code);
					else if ((code = sa_shutdown (sd, "w")) != SA_OK)
						log_out (lg, LV_ERROR, "Failed to shutdown connection %d", code);
					else {
						len = sizeof (buf) - 1;
						ptr = buf;
						failcount = 0;
						while (len > 0) {
							code = sa_read (sd, ptr, len, & received);
							if (code == SA_ERR_EOF) {
								*ptr = '\0';
								rc = 0;
								break;
							} else if (code != SA_OK) {
								bool_t	retry = false;
								
								++failcount;
								log_out (lg, LV_ERROR, "Failed to read answer %d (%d)", code, failcount);
								if ((code == SA_ERR_TMT) || (code == SA_ERR_SYS) || (code == SA_ERR_NET))
									if (failcount <= 8)
										retry = true;
								if (! retry)
									break;
							} else {
								failcount = 0;
								len -= received;
								ptr += received;
							}
						}
						if (rc == 0)
							parse_exit_code (buf, exit_code);
					}
				}
			}
		}
		sa_destroy (sd);
	}
	sa_addr_destroy (addr);
	return rc;
}/*}}}*/
static int
usage (const char *pgm) /*{{{*/
{
	fprintf (stderr, "Usage: %s [-L <loglevel>]\n", pgm);
	return 1;
}/*}}}*/
int
main (int argc, char **argv) /*{{{*/
{
	int		rc;
	int		n;
	const char	*program;
	const char	*loglevel;
	int		input_timeout;
	int		comm_timeout;
	int		head_only;
	store_t		*st;
	csig_t		*csig;
	log_t		*lg;
	
	if (program = strrchr (argv[0], '/'))
		++program;
	else
		program = argv[0];
        loglevel = log_level_name (LV_ERROR);
	input_timeout = 60;
	comm_timeout = 120;
	while ((n = getopt (argc, argv, "L:i:c:")) != -1)
		switch (n) {
		case 'L':
			loglevel = optarg;
			break;
		case 'i':
			if ((input_timeout = atoi (optarg)) < 1)
				return fprintf (stderr, "[%s] Input timeout must be greater 0, not %d.\n", program, input_timeout), 1;
			break;
		case 'c':
			if ((comm_timeout = atoi (optarg)) < 1)
				return fprintf (stderr, "[%s] Communication timeout must be greater 0, not %d.\n", program, comm_timeout), 1;
			break;
		case '?':
		default:
			return usage (program);
		}
	if (optind < argc)
		return usage (program);
	head_only = (! strcmp (program, "is_no_systemmail"));
	if (! (st = store_alloc (256 * 1024)))
		return fprintf (stderr, "[%s] Unable to setup storing buffer (%m).\n", program), 1;
	rc = 0;
	if (csig = csig_alloc (SIGPIPE, SIG_IGN, -1)) {
		if (lg = log_alloc (NULL, program, loglevel)) {
			if (read_mail (st, lg, input_timeout, head_only) == -1) {
				log_out (lg, LV_ERROR, "Failed to read incoming mail");
				rc = 1;
			} else if (to_bav (st, lg, comm_timeout, program, & rc) == -1) {
				log_out (lg, LV_ERROR, "Failed to forward mail to bav");
				if (! rc)
					rc = 1;
			}
			csig_free (csig);
		} else
			return fprintf (stderr, "[%s] Unable to setup logging interface.\n", program), 1;
	} else
		return fprintf (stderr, "[%s] Unable to setup signal handler.\n", program), 1;
	store_free (st);
	return rc;
}/*}}}*/