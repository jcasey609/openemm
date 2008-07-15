#!/usr/bin/env python
#	-*- python -*-
"""**********************************************************************************
*  The contents of this file are subject to the OpenEMM Public License Version 1.1
*  ("License"); You may not use this file except in compliance with the License.
*  You may obtain a copy of the License at http://www.agnitas.org/openemm.
*  Software distributed under the License is distributed on an "AS IS" basis,
*  WITHOUT WARRANTY OF ANY KIND, either express or implied.  See the License for
*  the specific language governing rights and limitations under the License.
* 
*  The Original Code is OpenEMM.
*  The Initial Developer of the Original Code is AGNITAS AG. Portions created by
*  AGNITAS AG are Copyright (C) 2006 AGNITAS AG. All Rights Reserved.
* 
*  All copies of the Covered Code must include on each user interface screen,
*  visible to all users at all times
*     (a) the OpenEMM logo in the upper left corner and
*     (b) the OpenEMM copyright notice at the very bottom center
*  See full license, exhibit B for requirements.
**********************************************************************************


"""
#
import	BaseHTTPServer, cgi
import	sys, os, signal, time, sre, types, mutex
import	fnmatch, gdbm
import	email, email.Header
import	agn
agn.require ('1.4.2')
#
agn.loglevel = agn.LV_DEBUG
#
alock = mutex.mutex ()
class Autoresponder:

	msgPattern = agn.base + os.sep + 'var' + os.sep + 'spool' + os.sep + 'bav' + os.sep + 'ar_%s.mail'
	limitPattern = agn.base + os.sep + 'var' + os.sep + 'spool' + os.sep + 'bav' + os.sep + 'ar_%s.limit'
	wlFile = agn.base + os.sep + 'conf' + os.sep + 'bav' + os.sep + 'ar.whitelist'
	wlPattern = agn.base + os.sep + 'conf' + os.sep + 'bav' + os.sep + 'ar_%s.whitelist'
	
	def __init__ (self, id, sender):
		self.id = id
		self.sender = sender
	
	def isInBlacklist (self, company_id):
		accept = True
		db = agn.DBase ()
		if db:
			i = db.newInstance ()
			if i:
				for state in [ 0, 1 ]:
					table = None
					if state == 0:
						table = 'cust_ban_tbl';
					elif state == 1:
						if company_id:
							table = 'cust%s_ban_tbl' % company_id
					if not table:
						continue
					(rc, data) = i.queryAll ('SELECT email FROM ' + table)
					if rc:
						for email in data:
							if '_' in email[0] or '%' in email[0]:
								pattern = email[0].replace ('%', '*').replace ('_', '?')
								if fnmatch.fnmatch (self.sender, pattern):
									accept = False
							elif email[0] == self.sender:
								accept = False
							if not accept:
								agn.log (agn.LV_INFO, 'blist', 'Autoresponder disabled due to blacklist entry "%s" on %s' % (email[0], table))
								break
						if not accept:
							break
					else:
						agn.log (agn.LV_WARNING, 'blist', 'Unable to read data from ' + table)
				i.close ()
			else:
				agn.log (agn.LV_ERROR, 'blist', 'Unable to get cursor for blacklisting')
			db.close ()
		else:
			agn.log (agn.LV_ERROR, 'blist', 'Unable to setup database interface for blacklisting')
		return not accept

	def createMessage (self, orig, parm):
		global	alock

		fname = Autoresponder.msgPattern % self.id
		if not os.access (fname, os.R_OK):
			agn.log (agn.LV_WARNING, 'ar', 'No autoresponder mail %s for enabled autoresponder %s found' % (fname, self.id))
			return None
		mayReceive = False
		for arwlist in [Autoresponder.wlPattern % self.id, Autoresponder.wlFile]:
			try:
				fd = open (arwlist)
				for line in [agn.chop (l) for l in fd.readlines () if not l[0] in '\n#']:
					if line == self.sender:
						mayReceive = True
						agn.log (agn.LV_VERBOSE, 'ar', 'Sender %s is on whitelist file %s' % (self.sender, arwlist))
						break
				fd.close ()
			except IOError:
				pass
			if mayReceive:
				break
		if not mayReceive:
			hasLock = False
			retry = 5
			while retry >= 0:
				if alock.testandset ():
					hasLock = True
					break
				time.sleep (1)
				retry -= 1
			if hasLock:
				try:
					arlimit = Autoresponder.limitPattern % self.id
					now = time.time ()
					dbf = gdbm.open (arlimit, 'c')
					if not dbf.has_key (self.sender):
						agn.log (agn.LV_DEBUG, 'ar', 'Never sent mail to %s from this autoresponder %s' % (self.sender, self.id))
						mayReceive = True
					else:
						try:
							last = int (dbf[self.sender])
							if last + 24 * 60 * 60 < now:
								agn.log (agn.LV_DEBUG, 'ar', 'Last mail to "%s" is older than 24 hours' % self.sender)
								mayReceive = True
							else:
								diff = (now - last) / 60
								agn.log (agn.LV_INFO, 'ar', 'Reject mail to "%s", sent already mail in last 24 hours (%d:%02d)' % (self.sender, diff / 60, diff % 60))
						except ValueError:
							pass
					if mayReceive:
						dbf[self.sender] = '%d' % now
					dbf.close ()
				except gdbm.error, e:
					agn.log (agn.LV_ERROR, 'ar', 'Unable to acess %s %s' % (arlimit, `e.args`))
					mayReceive = False
				alock.unlock ()
			else:
				agn.log (agn.LV_WARNING, 'ar', 'Unable to get global lock for %s' % self.id)
			if not mayReceive:
				return None
		if parm.has_key ('cid'):
			cid = parm['cid']
		else:
			cid = None
		if self.isInBlacklist (cid):
			agn.log (agn.LV_INFO, 'ar', 'Sender %s is blacklisted' % self.sender)
			return None
		try:
			fd = open (fname)
			armail = fd.read ()
			fd.close ()
			armsg = email.message_from_string (armail)
			armsg['To'] = '<%s>' % self.sender
			if not armsg.has_key ('subject') and orig.has_key ('subject'):
				subj = orig['subject']
				if len (subj) < 4 or subj[:4].lower () != 're: ':
					subj = 'Re: ' + subj
				armsg['Subject'] = subj
		except IOError, e:
			armsg = None
			agn.log (agn.LV_ERROR, 'ar', 'Unable to read %s %s' % (armail, `e.args`))
		return armsg
#
class Entry:
	def __init__ (self, line):
		self.id = line
		self.parm = None
		if line[0] == '{':
			n = line.find ('}')
			if n != -1:
				self.parm = line[1:n]
				line = line[n + 1:]
		self.pattern = line
		self.regexp = sre.compile (self.pattern, sre.IGNORECASE)
	
	def match (self, line):
		if self.regexp.search (line):
			return True
		return False

class Section:
	def __init__ (self, name):
		self.name = name
		self.entries = []
	
	def append (self, line):
		try:
			self.entries.append (Entry (line))
		except sre.error:
			agn.log (agn.LV_ERROR, 'section', 'Got illegal regular expression "%s" in [%s]' % (line, self.name))

	def match (self, line):
		for e in self.entries:
			if e.match (line):
				return e
		return None

class Scan:
	def __init__ (self):
		self.section = None
		self.entry = None
		self.reason = None
		self.dsn = None
		self.etext = None
		self.minfo = None
	
	def __str__ (self):
		rc = '[ Section: '
		if self.section:
			rc += self.section.name
		else:
			rc += '*none*'
		rc += ', Entry: '
		if self.entry:
			rc += self.entry.id
		else:
			rc += '*none*'
		rc += ', Reason: '
		if self.reason:
			rc += self.reason
		else:
			rc += '*none*'
		rc += ', DSN: '
		if self.dsn:
			rc += self.dsn
		else:
			rc += '*none*'
		rc += ', EText: '
		if self.etext:
			rc += self.etext
		else:
			rc += '*none*'
		rc += ', MInfo: '
		if self.minfo:
			rc += `self.minfo`
		else:
			rc += '*none*'
		rc += ' ]'
		return rc

class Rule:
	lifetime = 180

	rulePattern = agn.base + os.sep + 'conf' + os.sep + 'bav' + os.sep + 'bav_%s.rule'
	ruleFile = agn.base + os.sep + 'conf' + os.sep + 'bav' + os.sep + 'bav.rule'
	DSNRE = (sre.compile ('[45][0-9][0-9] +([0-9]\\.[0-9]\\.[0-9]) +(.*)'),
		 sre.compile ('\\(#([0-9]\\.[0-9]\\.[0-9])\\)'),
		 sre.compile ('^([0-9]\\.[0-9]\\.[0-9])'))
	NMidRE = sre.compile ('<([0-9]{14}-[0-9]+\\.[0-9a-z]+\\.[0-9a-z]+\\.[0-9a-z]+\\.[0-9a-z]+\\.[0-9a-z]+)@')
	
	def __init__ (self, rid, now):
		self.created = now
		self.sections = {}
		self.passwords = {}
		for fname in [Rule.rulePattern % rid, Rule.ruleFile]:
			try:
				fd = open (fname, 'r')
				agn.log (agn.LV_DEBUG, 'rule', 'Reading rules from %s' % fname)
			except IOError, e:
				agn.log (agn.LV_VERBOSE, 'rule', 'Unable to open %s %s' % (fname, `e.args`))
				fd = None
			if fd:
				break
		if fd:
			cur = None
			for line in [agn.chop (l) for l in fd if len (l) > 0 and not l[0] in '\n#']:
				if line[0] == '[' and line[-1] == ']':
					name = line[1:-1]
					if self.sections.has_key (name):
						cur = self.sections[name]
					else:
						cur = Section (name)
						self.sections[name] = cur
				elif cur:
					cur.append (line)
	
	def __collectSections (self, use):
		rc = []
		if type (use) in types.StringTypes:
			use = [use]
		for u in use:
			if self.sections.has_key (u):
				rc.append (self.sections[u])
		return rc
	
	def __decode (self, h):
		rc = ''
		for dc in email.Header.decode_header (h):
			if rc:
				rc += ' '
			rc += dc[0]
		return rc.replace ('\n', ' ')

	def __match (self, line, sects):
		sec = None
		entry = None
		for s in sects:
			entry = s.match (line)
			if entry:
				sec = s
				break
		return (sec, entry)
	
	def __checkHeader (self, msg, sects):
		rc = None
		for key in msg.keys ():
			line = key + ': ' + self.__decode (msg[key])
			(sec, ent) = self.__match (line, sects)
			if sec:
				reason = '[%s/%s] %s' % (sec.name, ent.id, line)
				rc = (sec, ent, reason)
				break
		return rc

	def matchHeader (self, msg, use):
		return self.__checkHeader (msg, self.__collectSections (use))
	
	def __scanMID (self, scan, mid, where):
		if mid and not scan.minfo:
			mt = Rule.NMidRE.search (mid)
			if mt:
				grps = mt.groups ()
				try:
					uid = agn.UID ()
					uid.parseUID (grps[0])
					uid.password = None
					if self.passwords.has_key (uid.companyID):
						uid.password = self.passwords[uid.companyID]
					else:
						db = agn.DBase ()
						if db:
							inst = db.newInstance ()
							if inst:
								rec = inst.simpleQuery ('SELECT xor_key FROM company_tbl WHERE company_id = %d' % uid.companyID)
								if not rec is None and not rec[0] is None:
									if type (rec[0]) in types.StringTypes:
										uid.password = rec[0]
									else:
										uid.password = str (rec[0])
									self.passwords[uid.companyID] = uid.password
							else:
								agn.log (agn.LV_ERROR, 'mid', 'Unable to get databse cursor')
							db.close ()
						else:
							agn.log (agn.LV_ERROR, 'mid', 'Unable to create database')
				except agn.error, e:
					agn.log (agn.LV_ERROR, 'mid', 'Failed: ' + e.msg)
				if not uid.password is None:
					if uid.validateUID ():
						scan.minfo = (uid.mailingID, uid.customerID)
						agn.log (agn.LV_INFO, 'mid', 'Found new message id in %s: %s' % (where, mid))
					else:
						agn.log (agn.LV_WARNING, 'mid', 'Found invalid new message id in %s: %s' % (where, mid))

	def __scan (self, msg, scan, sects, checkheader):
		if checkheader:
			if not scan.section:
				rc = self.__checkHeader (msg, sects)
				if rc:
					(scan.section, scan.entry, scan.reason) = rc
			self.__scanMID (scan, msg['message-id'], 'header')
		if not scan.dsn:
			subj = msg['subject']
			if subj:
				mt = Rule.DSNRE[0].search (self.__decode (subj))
				if mt:
					grps = mt.groups ()
					scan.dsn = grps[0]
					scan.etext = grps[1]
					agn.log (agn.LV_VERBOSE, 'dsn', 'Found DSN in Subject: %s' % subj)
			if not scan.dsn:
				action = msg['action']
				status = msg['status']
				if action and status:
					mt = Rule.DSNRE[2].match (self.__decode (status))
					if mt:
						scan.dsn = mt.groups ()[0]
						scan.etext = 'Action: ' + action
						agn.log (agn.LV_VERBOSE, 'dsn', 'Found DSN in Action: %s / Status: %s' % (action, status))
		pl = msg.get_payload (decode = True)
		if not pl:
			pl = msg.get_payload ()
		if type (pl) in types.StringTypes:
			for line in pl.split ('\n'):
				if not scan.minfo:
					self.__scanMID (scan, line, 'body')
				if not scan.section:
					(sec, ent) = self.__match (line, sects)
					if sec:
						scan.section = sec
						scan.entry = ent
						scan.reason = line
						agn.log (agn.LV_VERBOSE, 'match', 'Found pattern "%s" in body "%s"' % (scan.entry.pattern, line))
				if not scan.dsn:
					mt = Rule.DSNRE[0].search (line)
					if mt:
						grps = mt.groups ()
						scan.dsn = grps[0]
						scan.etext = grps[1]
						agn.log (agn.LV_VERBOSE, 'dsn', 'Found DSN %s / Text "%s" in body: %s' % (scan.dsn, scan.etext, line))
					else:
						mt = Rule.DSNRE[1].search (line)
						if mt:
							scan.dsn = mt.groups ()[0]
							scan.etext = line
							agn.log (agn.LV_VERBOSE, 'dsn', 'Found DSN %s in body: %s' % (scan.dsn, line))
		elif type (pl) in [ types.TupleType, types.ListType ]:
			for p in pl:
				self.__scan (p, scan, sects, True)
				if scan.section and scan.dsn and scan.minfo:
					break
	
	def scanMessage (self, msg, use):
		rc = Scan ()
		sects = self.__collectSections (use)
		self.__scan (msg, rc, sects, False)
		if rc.section:
			if not rc.dsn:
				if rc.section.name == 'hard':
					rc.dsn = '5.9.9'
				else:
					rc.dsn = '4.9.9'
		if not rc.etext:
			if rc.reason:
				rc.etext = rc.reason
			else:
				rc.etext = ''
		return rc
#
rules = {}
rlock = mutex.mutex ()
class BAV:
	x_agn = 'X-AGNMailloop'

	configFile = agn.base + os.sep + 'var' + os.sep + 'spool' + os.sep + 'bav' + os.sep + 'bav.conf'
	savePattern = agn.base + os.sep + 'var' + os.sep + 'spool' + os.sep + 'filter' + os.sep + '%s-%s'
	extBouncelog = agn.base + os.sep + 'var' + os.sep + 'spool' + os.sep + 'log' + os.sep + 'extbounce.log'
	
	def __init__ (self, msg, mode):
		global	rules, rlock

		self.msg = msg
		self.mode = mode
		self.parm = {}
		if not self.msg.has_key (BAV.x_agn):
			if self.msg.has_key ('return-path'):
				rp = self.msg['return-path'].strip ()
				if rp[0] == '<' and rp[-1] == '>':
					addr = rp[1:-1].lower ()
					try:
						fd = open (BAV.configFile)
						for line in [l for l in fd if len (l) > 0 and l[0] != '#']:
							parts = line.split (None, 1)
							if parts[0].lower () == addr:
								data = agn.chop (parts[1])
								if data[:7] == 'accept:':
									self.msg[BAV.x_agn] = data[7:]
								break
						fd.close ()
					except IOError, e:
						agn.log (agn.LV_WARNING, 'bav', 'Cannot read file %s %s' % (BAV.configFile, `e.args`))
			else:
				agn.log (agn.LV_WARNING, 'bav', 'No %s header, neither Return-Path: found' % BAV.x_agn)
		if self.msg.has_key (BAV.x_agn):
			for pair in self.msg[BAV.x_agn].split (','):
				(var, val) = pair.split ('=', 1)
				self.parm[var.strip ()] = val
		try:
			rid = self.parm['rid']
		except KeyError:
			rid = 'unspec'
		self.rid = rid
		try:
			sender = self.parm['from']
			if len (sender) > 1 and sender[0] == '<' and sender[-1] == '>':
				sender = sender[1:-1]
				if sender == '':
					sender = 'MAILER-DAEMON'
		except KeyError:
			sender = 'postmaster'
		self.sender = sender
		if not msg.get_unixfrom ():
			msg.set_unixfrom (time.strftime ('From ' + sender + '  %c'))
		now = time.time ()
		if not rules.has_key (rid):
			self.rule = Rule (rid, now)
			if rlock.testandset ():
				rules[rid] = self.rule
				rlock.unlock ()
		else:
			self.rule = rules[rid]
			if self.rule.created + Rule.lifetime < now:
				self.rule = Rule (rid, now)
				if rlock.testandset ():
					rules[rid] = self.rule
					rlock.unlock ()
		self.reason = ''

	def saveMessage (self, id):
		fname = BAV.savePattern % (id, self.rid)
		try:
			fd = open (fname, 'a')
			fd.write (self.msg.as_string (True) + '\n')
			fd.close ()
		except IOError, e:
			agn.log (agn.LV_ERROR, 'save', 'Unable to save mail copy to %s %s' % (fname, `e.args`))
	
	def sendmail (self, msg, to):
		try:
			pp = os.popen ('/usr/sbin/sendmail ' + to, 'w')
			pp.write (msg.as_string (False))
			pp.close ()
		except Exception, e:
			agn.log (agn.LV_ERROR, 'sendmail', 'Sending mail to %s failed %s' % (to, `e.args`))
	
	def execute_is_no_systemmail (self):
		if self.rule.matchHeader (self.msg, 'systemmail'):
			return False
		return True
	
	def execute_filter_or_forward (self):
		match = self.rule.matchHeader (self.msg, 'filter')
		if match:
			if not match[1].parm:
				parm = 'save'
			else:
				parm = match[1].parm
		else:
			parm = 'sent'
		self.saveMessage (parm)
		if not match:
			while self.msg.has_key (BAV.x_agn):
				del self.msg[BAV.x_agn]
			if self.parm.has_key ('fwd'):
				self.sendmail (self.msg, self.parm['fwd'])
			if self.parm.has_key ('ar'):
				ar = self.parm['ar']
				if self.parm.has_key ('from'):
					sender = email.Utils.parseaddr (self.msg['from'])[1].lower ()
					if sender:
						ar = Autoresponder (ar, sender)
						nmsg = ar.createMessage (self.msg, self.parm)
						if nmsg:
							agn.log (agn.LV_INFO, 'fof', 'Forward newly generated message to %s' % sender)
							self.sendmail (nmsg, sender)
					else:
						agn.log (agn.LV_INFO, 'fof', 'No email in sender "%s" found' % self.msg['from'])
				else:
					agn.log (agn.LV_INFO, 'fof', 'No sender in original message found')
		return True

	def execute_scan_and_unsubscribe (self):
		scan = self.rule.scanMessage (self.msg, ['hard', 'soft'])
		if scan and scan.section and scan.minfo:
			try:
				fd = open (BAV.extBouncelog, 'a')

				fd.write ('%s;0;%d;0;%d;mailloop=%s\n' % (scan.dsn, scan.minfo[0], scan.minfo[1], scan.etext))
				fd.close ()
			except IOError, e:
				agn.log (agn.LV_ERROR, 'log', 'Unable to write %s %s' % (BAV.extBouncelog, `e.args`))
		if scan.entry and scan.entry.parm:
			parm = scan.entry.parm
		else:
			parm = 'unspec'
		self.saveMessage (parm)
		return True

	def execute (self):
		if self.mode == 0:
			return self.execute_is_no_systemmail ()
		elif self.mode == 1:
			return self.execute_filter_or_forward ()
		elif self.mode == 2:
			return self.execute_scan_and_unsubscribe ()
		self.reason = 'Invalid mode %d' % self.mode
		return False
#
class Request (BaseHTTPServer.BaseHTTPRequestHandler):
	def out (self, str):
		self.wfile.write (str)
		self.wfile.flush ()

	def err (self, str):
		self.out ('-ERR: ' + str + '\r\n')
	
	def ok (self, str):
		self.out ('+OK: ' + str + '\r\n')
	
	def data (self, data):
		self.out ('*DATA %d\r\n%s' % (len (data), data))
	
	def startup (self, isPost):
		path = self.path
		agn.log (agn.LV_VERBOSE, 'startup', 'Got path: ' + path)
		n = path.find ('?')
		if n != -1:
			query = cgi.parse_qs (path[n + 1:], True)
			path = path[:n]
		else:
			query = None
		body = None
		if isPost:
			if self.headers and self.headers.has_key ('content-length'):
				try:
					body = self.rfile.read (int (self.headers['content-length']))
				except ValueError, e:
					pass
			if not body:
				body = self.rfile.read ()
		self.out ('HTTP/1.0 200 OK\r\n'
			  'Content-Type: text/plain\r\n'
			  '\r\n')
		return (path, query, body)
	
	def doit (self, isPost):
		(path, query, body) = self.startup (isPost)
		if path == '/ping':
			self.ok ('pong')
		else:
			if path == '/is_no_systemmail':
				mode = 0
			elif path == '/filter_or_forward':
				mode = 1
			elif path == '/scan_and_unsubscribe':
				mode = 2
			else:
				mode = -1
			if mode != -1:
				if body:
					msg = email.message_from_string (body)
					bav = BAV (msg, mode)
					if bav.execute ():
						self.ok (bav.reason)
					else:
						self.err (bav.reason)
				else:
					agn.log (agn.LV_ERROR, 'doit', 'No body for path %s found' % path)
					self.err ('%s is only allowed using POST' % path)
			else:
				agn.log (agn.LV_WARNING, 'doit', 'Invalid command/path ' + self.path)
				self.err ('Not (yet) implemented')

	def do_POST (self):
		self.doit (True)
	
	def do_GET (self):
		self.doit (False)

class Server (BaseHTTPServer.SocketServer.ThreadingMixIn, BaseHTTPServer.HTTPServer):
	def __init__ (self):
		BaseHTTPServer.HTTPServer.__init__ (self, ('127.0.0.1', 5166), Request)

def handler (sig, stack):
	agn.log (agn.LV_INFO, 'bavd', 'Going down')
	sys.exit (0)

signal.signal (signal.SIGTERM, handler)
signal.signal (signal.SIGINT, handler)
signal.signal (signal.SIGHUP, signal.SIG_IGN)
agn.log (agn.LV_INFO, 'bavd', 'Starting up')
server = Server ()
server.serve_forever ()